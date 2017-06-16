package org.thoughtcrime.securesms;

import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.widget.TextView;

import org.thoughtcrime.securesms.crypto.MasterSecret;
import org.thoughtcrime.securesms.crypto.storage.TextSecureIdentityKeyStore;
import org.thoughtcrime.securesms.crypto.storage.TextSecureSessionStore;
import org.thoughtcrime.securesms.database.DatabaseFactory;
import org.thoughtcrime.securesms.database.MmsAddressDatabase;
import org.thoughtcrime.securesms.database.MmsDatabase;
import org.thoughtcrime.securesms.database.MmsSmsDatabase;
import org.thoughtcrime.securesms.database.PushDatabase;
import org.thoughtcrime.securesms.database.SmsDatabase;
import org.thoughtcrime.securesms.database.documents.IdentityKeyMismatch;
import org.thoughtcrime.securesms.database.model.MessageRecord;
import org.thoughtcrime.securesms.jobs.PushDecryptJob;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.recipients.RecipientFactory;
import org.thoughtcrime.securesms.recipients.Recipients;
import org.thoughtcrime.securesms.sms.MessageSender;
import org.thoughtcrime.securesms.util.Base64;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.VerifySpan;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.state.IdentityKeyStore;
import org.whispersystems.signalservice.api.messages.SignalServiceEnvelope;
import org.whispersystems.signalservice.api.util.InvalidNumberException;
import org.whispersystems.signalservice.internal.push.SignalServiceProtos;

import java.io.IOException;

import static org.whispersystems.libsignal.SessionCipher.SESSION_LOCK;

public class ConfirmIdentityDialog extends AlertDialog {

  private static final String TAG = ConfirmIdentityDialog.class.getSimpleName();

  private OnClickListener callback;

  public ConfirmIdentityDialog(Context context,
                               MasterSecret masterSecret,
                               MessageRecord messageRecord,
                               IdentityKeyMismatch mismatch)
  {
    super(context);

    try {
      Recipient       recipient       = RecipientFactory.getRecipientForId(context, mismatch.getRecipientId(), false);
      String          name            = recipient.toShortString();
      String          number          = Util.canonicalizeNumber(context, recipient.getNumber());
      String          introduction    = String.format(context.getString(R.string.ConfirmIdentityDialog_your_safety_number_with_s_has_changed), name, name);
      SpannableString spannableString = new SpannableString(introduction + " " +
                                                            context.getString(R.string.ConfirmIdentityDialog_you_may_wish_to_verify_your_safety_number_with_this_contact));

      spannableString.setSpan(new VerifySpan(context, mismatch),
                              introduction.length()+1, spannableString.length(),
                              Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

      setTitle(name);
      setMessage(spannableString);

      setButton(AlertDialog.BUTTON_POSITIVE, context.getString(R.string.ConfirmIdentityDialog_accept), new AcceptListener(masterSecret, messageRecord, mismatch, number));
      setButton(AlertDialog.BUTTON_NEGATIVE, context.getString(android.R.string.cancel),               new CancelListener());
    } catch (InvalidNumberException e) {
      throw new AssertionError(e);
    }
  }

  @Override
  public void show() {
    super.show();
    ((TextView)this.findViewById(android.R.id.message))
                   .setMovementMethod(LinkMovementMethod.getInstance());
  }

  public void setCallback(OnClickListener callback) {
    this.callback = callback;
  }

  private class AcceptListener implements OnClickListener {

    private final MasterSecret        masterSecret;
    private final MessageRecord       messageRecord;
    private final IdentityKeyMismatch mismatch;
    private final String              number;

    private AcceptListener(MasterSecret masterSecret, MessageRecord messageRecord, IdentityKeyMismatch mismatch, String number) {
      this.masterSecret  = masterSecret;
      this.messageRecord = messageRecord;
      this.mismatch      = mismatch;
      this.number        = number;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
      new AsyncTask<Void, Void, Void>()
      {
        @Override
        protected Void doInBackground(Void... params) {
          synchronized (SESSION_LOCK) {
            SignalProtocolAddress      mismatchAddress  = new SignalProtocolAddress(number, 1);
            TextSecureIdentityKeyStore identityKeyStore = new TextSecureIdentityKeyStore(getContext());

            identityKeyStore.saveIdentity(mismatchAddress, mismatch.getIdentityKey(), true);
          }

          processMessageRecord(messageRecord);
          processPendingMessageRecords(messageRecord.getThreadId(), mismatch);

          return null;
        }

        private void processMessageRecord(MessageRecord messageRecord) {
          if (messageRecord.isOutgoing()) processOutgoingMessageRecord(messageRecord);
          else                            processIncomingMessageRecord(messageRecord);
        }

        private void processPendingMessageRecords(long threadId, IdentityKeyMismatch mismatch) {
          MmsSmsDatabase        mmsSmsDatabase = DatabaseFactory.getMmsSmsDatabase(getContext());
          Cursor                cursor         = mmsSmsDatabase.getIdentityConflictMessagesForThread(threadId);
          MmsSmsDatabase.Reader reader         = mmsSmsDatabase.readerFor(cursor, masterSecret);
          MessageRecord         record;

          try {
            while ((record = reader.getNext()) != null) {
              for (IdentityKeyMismatch recordMismatch : record.getIdentityKeyMismatches()) {
                if (mismatch.equals(recordMismatch)) {
                  processMessageRecord(record);
                }
              }
            }
          } finally {
            if (reader != null)
              reader.close();
          }
        }

        private void processOutgoingMessageRecord(MessageRecord messageRecord) {
          SmsDatabase        smsDatabase        = DatabaseFactory.getSmsDatabase(getContext());
          MmsDatabase        mmsDatabase        = DatabaseFactory.getMmsDatabase(getContext());
          MmsAddressDatabase mmsAddressDatabase = DatabaseFactory.getMmsAddressDatabase(getContext());

          if (messageRecord.isMms()) {
            mmsDatabase.removeMismatchedIdentity(messageRecord.getId(),
                                                 mismatch.getRecipientId(),
                                                 mismatch.getIdentityKey());

            Recipients recipients = mmsAddressDatabase.getRecipientsForId(messageRecord.getId());

            if (recipients.isGroupRecipient()) MessageSender.resendGroupMessage(getContext(), masterSecret, messageRecord, mismatch.getRecipientId());
            else                               MessageSender.resend(getContext(), masterSecret, messageRecord);
          } else {
            smsDatabase.removeMismatchedIdentity(messageRecord.getId(),
                                                 mismatch.getRecipientId(),
                                                 mismatch.getIdentityKey());

            MessageSender.resend(getContext(), masterSecret, messageRecord);
          }
        }

        private void processIncomingMessageRecord(MessageRecord messageRecord) {
          try {
            PushDatabase pushDatabase = DatabaseFactory.getPushDatabase(getContext());
            SmsDatabase  smsDatabase  = DatabaseFactory.getSmsDatabase(getContext());

            smsDatabase.removeMismatchedIdentity(messageRecord.getId(),
                                                 mismatch.getRecipientId(),
                                                 mismatch.getIdentityKey());

            boolean legacy = !messageRecord.isContentBundleKeyExchange();

            SignalServiceEnvelope envelope = new SignalServiceEnvelope(SignalServiceProtos.Envelope.Type.PREKEY_BUNDLE_VALUE,
                                                                       messageRecord.getIndividualRecipient().getNumber(),
                                                                       messageRecord.getRecipientDeviceId(), "",
                                                                       messageRecord.getDateSent(),
                                                                       legacy ? Base64.decode(messageRecord.getBody().getBody()) : null,
                                                                       !legacy ? Base64.decode(messageRecord.getBody().getBody()) : null);

            long pushId = pushDatabase.insert(envelope);

            ApplicationContext.getInstance(getContext())
                              .getJobManager()
                              .add(new PushDecryptJob(getContext(), pushId, messageRecord.getId(),
                                                      messageRecord.getIndividualRecipient().getNumber()));
          } catch (IOException e) {
            throw new AssertionError(e);
          }
        }

      }.execute();

      if (callback != null) callback.onClick(null, 0);
    }
  }

  private class CancelListener implements OnClickListener {
    @Override
    public void onClick(DialogInterface dialog, int which) {
      if (callback != null) callback.onClick(null, 0);
    }
  }

}
