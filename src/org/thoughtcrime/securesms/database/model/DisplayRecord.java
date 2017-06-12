/**
 * Copyright (C) 2012 Moxie Marlinspike
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.thoughtcrime.securesms.database.model;

import android.content.Context;
import android.text.SpannableString;

import org.thoughtcrime.securesms.database.MmsSmsColumns;
import org.thoughtcrime.securesms.database.SmsDatabase;
import org.thoughtcrime.securesms.recipients.Recipients;

/**
 * The base class for all message record models.  Encapsulates basic data
 * shared between ThreadRecord and MessageRecord.
 *
 * @author Moxie Marlinspike
 *
 */

public abstract class DisplayRecord {

  protected final Context context;
  protected final long type;

  private final Recipients recipients;
  private final long       dateSent;
  private final long       dateReceived;
  private final long       threadId;
  private final Body       body;
  private final int        deliveryStatus;
  private final int        receiptCount;

  public DisplayRecord(Context context, Body body, Recipients recipients, long dateSent,
                       long dateReceived, long threadId, int deliveryStatus, int receiptCount, long type)
  {
    this.context              = context.getApplicationContext();
    this.threadId             = threadId;
    this.recipients           = recipients;
    this.dateSent             = dateSent;
    this.dateReceived         = dateReceived;
    this.type                 = type;
    this.body                 = body;
    this.receiptCount         = receiptCount;
    this.deliveryStatus       = deliveryStatus;
  }

  public Body getBody() {
    return body;
  }

  public boolean isFailed() {
    return
        MmsSmsColumns.Types.isFailedMessageType(type)            ||
        MmsSmsColumns.Types.isPendingSecureSmsFallbackType(type) ||
        deliveryStatus >= SmsDatabase.Status.STATUS_FAILED;
  }

  public boolean isPending() {
    return MmsSmsColumns.Types.isPendingMessageType(type) &&
           !MmsSmsColumns.Types.isIdentityVerified(type)  &&
           !MmsSmsColumns.Types.isIdentityDefault(type);
  }

  public boolean isOutgoing() {
    return MmsSmsColumns.Types.isOutgoingMessageType(type);
  }

  public abstract SpannableString getDisplayBody();

  public Recipients getRecipients() {
    return recipients;
  }

  public long getDateSent() {
    return dateSent;
  }

  public long getDateReceived() {
    return dateReceived;
  }

  public long getThreadId() {
    return threadId;
  }

  public boolean isKeyExchange() {
    return SmsDatabase.Types.isKeyExchangeType(type);
  }

  public boolean isEndSession() {
    return SmsDatabase.Types.isEndSessionType(type);
  }

  public boolean isGroupUpdate() {
    return SmsDatabase.Types.isGroupUpdate(type);
  }

  public boolean isGroupQuit() {
    return SmsDatabase.Types.isGroupQuit(type);
  }

  public boolean isGroupAction() {
    return isGroupUpdate() || isGroupQuit();
  }

  public boolean isExpirationTimerUpdate() {
    return SmsDatabase.Types.isExpirationTimerUpdate(type);
  }

  public boolean isCallLog() {
    return SmsDatabase.Types.isCallLog(type);
  }

  public boolean isJoined() {
    return SmsDatabase.Types.isJoinedType(type);
  }

  public boolean isIncomingCall() {
    return SmsDatabase.Types.isIncomingCall(type);
  }

  public boolean isOutgoingCall() {
    return SmsDatabase.Types.isOutgoingCall(type);
  }

  public boolean isMissedCall() {
    return SmsDatabase.Types.isMissedCall(type);
  }

  public int getDeliveryStatus() {
    return deliveryStatus;
  }

  public int getReceiptCount() {
    return receiptCount;
  }

  public boolean isDelivered() {
    return (deliveryStatus >= SmsDatabase.Status.STATUS_COMPLETE &&
            deliveryStatus < SmsDatabase.Status.STATUS_PENDING) || receiptCount > 0;
  }

  public boolean isPendingInsecureSmsFallback() {
    return SmsDatabase.Types.isPendingInsecureSmsFallbackType(type);
  }

  public static class Body {
    private final String body;
    private final boolean plaintext;

    public Body(String body, boolean plaintext) {
      this.body      = body;
      this.plaintext = plaintext;
    }

    public boolean isPlaintext() {
      return plaintext;
    }

    public String getBody() {
      return body == null ? "" : body;
    }
  }
}
