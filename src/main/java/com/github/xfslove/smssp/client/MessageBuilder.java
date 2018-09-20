package com.github.xfslove.smssp.client;

import com.github.xfslove.smsj.sms.dcs.SmsAlphabet;
import com.github.xfslove.smsj.sms.dcs.SmsMsgClass;
import com.github.xfslove.smssp.message.MessageProtocol;

public class MessageBuilder {

  private MessageProtocol protocol;
  private String[] phones;

  public static class Sms {

    private String text;
    private SmsAlphabet alphabet = SmsAlphabet.UCS2;
    private SmsMsgClass msgClass;
  }

  public static class Mms {

    private String transactionId;
    private String from;
    private int size;
    private String contentLocation;
    private int expiry = 7 * 24 * 60 * 60;
  }

  public static class Cmpp extends MessageBuilder {

    private String srcId;
    private String serviceId;
    private String msgSrc;

  }

  public static class Sgip extends MessageBuilder {

    private String spNumber;
    private String corpId;
    private String serviceType;
    private int morelatetoMTFlag = 3;

  }

  public MessageBuilder protocol(MessageProtocol protocol) {
    switch (protocol) {
      case SGIP_12:
        return new Sgip();
      case CMPP_20:
        return new Cmpp();
      default:
        return null;
    }
  }

  public MessageBuilder sms() {

  }


}