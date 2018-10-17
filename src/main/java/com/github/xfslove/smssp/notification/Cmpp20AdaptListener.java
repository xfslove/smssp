package com.github.xfslove.smssp.notification;

import com.github.xfslove.smssp.message.cmpp20.DeliverMessage;

/**
 * @author hanwen
 * created at 2018/10/17
 */
public abstract class Cmpp20AdaptListener implements NotificationListener {

  @Override
  public void done(Notification notification) {

    if (notification instanceof DeliverMessage) {
      done((DeliverMessage) notification);
    } else if (notification instanceof DeliverMessage.Report) {
      done((DeliverMessage.Report) notification);
    }
  }

  protected abstract void done(DeliverMessage deliverMessage);

  protected abstract void done(DeliverMessage.Report report);
}
