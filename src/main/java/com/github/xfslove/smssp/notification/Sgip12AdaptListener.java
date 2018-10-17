package com.github.xfslove.smssp.notification;

import com.github.xfslove.smssp.message.sgip12.DeliverMessage;
import com.github.xfslove.smssp.message.sgip12.ReportMessage;

/**
 * @author hanwen
 * created at 2018/10/17
 */
public abstract class Sgip12AdaptListener implements NotificationListener {

  @Override
  public void done(Notification notification) {

    if (notification instanceof DeliverMessage) {
      done((DeliverMessage) notification);
    } else if (notification instanceof ReportMessage) {
      done((ReportMessage) notification);
    }
  }

  protected abstract void done(DeliverMessage deliverMessage);

  protected abstract void done(ReportMessage reportMessage);
}
