package org.thoughtcrime.securesms.jobs;

import android.content.Context;
import android.util.Log;

import org.thoughtcrime.securesms.ApplicationContext;
import org.thoughtcrime.securesms.attachments.Attachment;
import org.thoughtcrime.securesms.crypto.MasterSecret;
import org.thoughtcrime.securesms.database.DatabaseFactory;
import org.thoughtcrime.securesms.database.MmsDatabase;
import org.thoughtcrime.securesms.database.NoSuchMessageException;
import org.thoughtcrime.securesms.database.documents.NetworkFailure;
import org.thoughtcrime.securesms.dependencies.InjectableType;
import org.thoughtcrime.securesms.jobs.requirements.MasterSecretRequirement;
import org.thoughtcrime.securesms.mms.MediaConstraints;
import org.thoughtcrime.securesms.mms.MmsException;
import org.thoughtcrime.securesms.mms.OutgoingGroupMediaMessage;
import org.thoughtcrime.securesms.mms.OutgoingMediaMessage;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.recipients.RecipientFactory;
import org.thoughtcrime.securesms.recipients.RecipientFormattingException;
import org.thoughtcrime.securesms.recipients.Recipients;
import org.thoughtcrime.securesms.transport.UndeliverableMessageException;
import org.thoughtcrime.securesms.util.GroupUtil;
import org.whispersystems.jobqueue.JobParameters;
import org.whispersystems.jobqueue.requirements.NetworkRequirement;
import org.whispersystems.signalservice.api.SignalServiceMessageSender;
import org.whispersystems.signalservice.api.crypto.UntrustedIdentityException;
import org.whispersystems.signalservice.api.messages.SignalServiceAttachment;
import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage;
import org.whispersystems.signalservice.api.messages.SignalServiceGroup;
import org.whispersystems.signalservice.api.push.SignalServiceAddress;
import org.whispersystems.signalservice.api.push.exceptions.EncapsulatedExceptions;
import org.whispersystems.signalservice.api.push.exceptions.NetworkFailureException;
import org.whispersystems.signalservice.api.util.InvalidNumberException;
import org.whispersystems.signalservice.internal.push.SignalServiceProtos.GroupContext;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

import static org.thoughtcrime.securesms.dependencies.SignalCommunicationModule.SignalMessageSenderFactory;

public class PushGroupSendJob extends PushSendJob implements InjectableType {

  private static final long serialVersionUID = 1L;

  private static final String TAG = PushGroupSendJob.class.getSimpleName();

  @Inject transient SignalMessageSenderFactory messageSenderFactory;

  private final long messageId;
  private final long filterRecipientId;

  public PushGroupSendJob(Context context, long messageId, String destination, long filterRecipientId) {
    super(context, JobParameters.newBuilder()
                                .withPersistence()
                                .withGroupId(destination)
                                .withRequirement(new MasterSecretRequirement(context))
                                .withRequirement(new NetworkRequirement(context))
                                .withRetryCount(5)
                                .create());

    this.messageId         = messageId;
    this.filterRecipientId = filterRecipientId;
  }

  @Override
  public void onAdded() {
  }

  @Override
  public void onPushSend(MasterSecret masterSecret)
      throws MmsException, IOException, NoSuchMessageException
  {
    MmsDatabase          database = DatabaseFactory.getMmsDatabase(context);
    OutgoingMediaMessage message  = database.getOutgoingMessage(masterSecret, messageId);

    try {
      deliver(masterSecret, message, filterRecipientId);

      database.markAsSent(messageId, true);
      markAttachmentsUploaded(messageId, message.getAttachments());

      if (message.getExpiresIn() > 0 && !message.isExpirationUpdate()) {
        database.markExpireStarted(messageId);
        ApplicationContext.getInstance(context)
                          .getExpiringMessageManager()
                          .scheduleDeletion(messageId, true, message.getExpiresIn());
      }
    } catch (InvalidNumberException | RecipientFormattingException | UndeliverableMessageException e) {
      Log.w(TAG, e);
      database.markAsSentFailed(messageId);
      notifyMediaMessageDeliveryFailed(context, messageId);
    } catch (EncapsulatedExceptions e) {
      Log.w(TAG, e);
      List<NetworkFailure> failures = new LinkedList<>();

      for (NetworkFailureException nfe : e.getNetworkExceptions()) {
        Recipient recipient = RecipientFactory.getRecipientsFromString(context, nfe.getE164number(), false).getPrimaryRecipient();
        failures.add(new NetworkFailure(recipient.getRecipientId()));
      }

      for (UntrustedIdentityException uie : e.getUntrustedIdentityExceptions()) {
        Recipient recipient = RecipientFactory.getRecipientsFromString(context, uie.getE164Number(), false).getPrimaryRecipient();
        database.addMismatchedIdentity(messageId, recipient.getRecipientId(), uie.getIdentityKey());
      }

      database.addFailures(messageId, failures);

      if (e.getNetworkExceptions().isEmpty() && e.getUntrustedIdentityExceptions().isEmpty()) {
        database.markAsSent(messageId, true);
        markAttachmentsUploaded(messageId, message.getAttachments());
      } else {
        database.markAsSentFailed(messageId);
        notifyMediaMessageDeliveryFailed(context, messageId);
      }
    }
  }

  @Override
  public boolean onShouldRetryThrowable(Exception exception) {
    if (exception instanceof IOException) return true;
    return false;
  }

  @Override
  public void onCanceled() {
    DatabaseFactory.getMmsDatabase(context).markAsSentFailed(messageId);
  }

  private void deliver(MasterSecret masterSecret, OutgoingMediaMessage message, long filterRecipientId)
      throws IOException, RecipientFormattingException, InvalidNumberException,
      EncapsulatedExceptions, UndeliverableMessageException
  {
    SignalServiceMessageSender    messageSender     = messageSenderFactory.create();
    byte[]                        groupId           = GroupUtil.getDecodedId(message.getRecipients().getPrimaryRecipient().getNumber());
    Recipients                    recipients        = DatabaseFactory.getGroupDatabase(context).getGroupMembers(groupId, false);
    MediaConstraints              mediaConstraints  = MediaConstraints.getPushMediaConstraints();
    List<Attachment>              scaledAttachments = scaleAttachments(masterSecret, mediaConstraints, message.getAttachments());
    List<SignalServiceAttachment> attachmentStreams = getAttachmentsFor(masterSecret, scaledAttachments);
    List<SignalServiceAddress>    addresses;

    if (filterRecipientId >= 0) addresses = getPushAddresses(filterRecipientId);
    else                        addresses = getPushAddresses(recipients);

    if (message.isGroup()) {
      OutgoingGroupMediaMessage groupMessage     = (OutgoingGroupMediaMessage) message;
      GroupContext              groupContext     = groupMessage.getGroupContext();
      SignalServiceAttachment   avatar           = attachmentStreams.isEmpty() ? null : attachmentStreams.get(0);
      SignalServiceGroup.Type   type             = groupMessage.isGroupQuit() ? SignalServiceGroup.Type.QUIT : SignalServiceGroup.Type.UPDATE;
      SignalServiceGroup        group            = new SignalServiceGroup(type, groupId, groupContext.getName(), groupContext.getMembersList(), avatar);
      SignalServiceDataMessage  groupDataMessage = new SignalServiceDataMessage(message.getSentTimeMillis(), group, null, null);

      messageSender.sendMessage(addresses, groupDataMessage);
    } else {
      SignalServiceGroup       group        = new SignalServiceGroup(groupId);
      SignalServiceDataMessage groupMessage = new SignalServiceDataMessage(message.getSentTimeMillis(), group,
                                                                           attachmentStreams, message.getBody(), false,
                                                                           (int)(message.getExpiresIn() / 1000),
                                                                           message.isExpirationUpdate());

      messageSender.sendMessage(addresses, groupMessage);
    }
  }

  private List<SignalServiceAddress> getPushAddresses(Recipients recipients) throws InvalidNumberException {
    List<SignalServiceAddress> addresses = new LinkedList<>();

    for (Recipient recipient : recipients.getRecipientsList()) {
      addresses.add(getPushAddress(recipient.getNumber()));
    }

    return addresses;
  }

  private List<SignalServiceAddress> getPushAddresses(long filterRecipientId) throws InvalidNumberException {
    List<SignalServiceAddress> addresses = new LinkedList<>();
    addresses.add(getPushAddress(RecipientFactory.getRecipientForId(context, filterRecipientId, false).getNumber()));
    return addresses;
  }

}
