package com.github.xfslove.smssp.client;

import com.github.xfslove.smsj.sms.SmsPdu;
import com.github.xfslove.smsj.sms.SmsTextMessage;
import com.github.xfslove.smsj.sms.dcs.SmsAlphabet;
import com.github.xfslove.smsj.sms.dcs.SmsMsgClass;
import com.github.xfslove.smsj.wap.mms.SmsMmsNotificationMessage;

import java.util.Arrays;

/**
 * <pre>
 * eg:
 *      Message message = Message
 *         .cmpp20()
 *         .msgSrc("src")
 *         .serviceId("serviceId")
 *         .srcId("srcId")
 *         .spNumber("10*****")
 *         .phones("186*****", "186*****")
 *         .sms()
 *         .text("text")
 *         .charset(SmsAlphabet.UCS2)
 *         .messageClass(SmsMsgClass.CLASS_0)
 *         .end();
 * </pre>
 *
 * @author hanwen
 * created at 2018/9/21
 */
public abstract class Message {

  private String spNumber;

  private String[] phones;

  private Pdu pdu;

  public static Sgip12 sgip12() {
    return new Sgip12();
  }

  public static Cmpp20 cmpp20() {
    return new Cmpp20();
  }

  public Message spNumber(String spNumber) {
    this.spNumber = spNumber;
    return this;
  }

  public Message phones(String... phones) {
    this.phones = phones;
    return this;
  }

  public Sms sms() {
    return new Sms();
  }

  public Mms mms() {
    return new Mms();
  }

  public String getSpNumber() {
    return spNumber;
  }

  public String[] getPhones() {
    return phones;
  }

  public Pdu getPdu() {
    return pdu;
  }

  public interface Pdu {

    SmsPdu[] convert();
  }

  public class Mms implements Pdu {

    private String transactionId;
    private String from;
    private int size;
    private String contentLocation;
    private int expiry = 7 * 24 * 60 * 60;

    public Mms transactionId(String transactionId) {
      this.transactionId = transactionId;
      return this;
    }

    public Mms from(String from) {
      this.from = from;
      return this;
    }

    public Mms size(int size) {
      this.size = size;
      return this;
    }

    public Mms contentLocation(String contentLocation) {
      this.contentLocation = contentLocation;
      return this;
    }

    public Mms expiry(int expiry) {
      this.expiry = expiry;
      return this;
    }

    public Message end() {
      Message.this.pdu = this;
      return Message.this;
    }

    @Override
    public SmsPdu[] convert() {
      SmsMmsNotificationMessage message = new SmsMmsNotificationMessage(contentLocation, size);
      message.setFrom(from + "/TYPE=PLMN");
      message.setTransactionId(transactionId);
      message.setExpiry(7 * 24 * 60 * 60);
      return message.getPdus();
    }

    @Override
    public String toString() {
      return "Mms{" +
          "transactionId='" + transactionId + '\'' +
          ", from='" + from + '\'' +
          ", size=" + size +
          ", contentLocation='" + contentLocation + '\'' +
          ", expiry=" + expiry +
          '}';
    }
  }

  public class Sms implements Pdu {

    private String text;
    private SmsAlphabet alphabet = SmsAlphabet.UCS2;
    private SmsMsgClass msgClass;

    public Sms text(String text) {
      this.text = text;
      return this;
    }

    public Sms charset(SmsAlphabet alphabet) {
      this.alphabet = alphabet;
      return this;
    }

    public Sms messageClass(SmsMsgClass msgClass) {
      this.msgClass = msgClass;
      return this;
    }

    public Message end() {
      Message.this.pdu = this;
      return Message.this;
    }

    @Override
    public SmsPdu[] convert() {
      return new SmsTextMessage(text, alphabet, msgClass).getPdus();
    }

    @Override
    public String toString() {
      return "Sms{" +
          "text='" + text + '\'' +
          ", alphabet=" + alphabet +
          ", msgClass=" + msgClass +
          '}';
    }
  }

  public static class Cmpp20 extends Message {

    private String srcId;
    private String serviceId;
    private String msgSrc;

    public Cmpp20 srcId(String srcId) {
      this.srcId = srcId;
      return this;
    }

    public Cmpp20 serviceId(String serviceId) {
      this.serviceId = serviceId;
      return this;
    }

    public Cmpp20 msgSrc(String msgSrc) {
      this.msgSrc = msgSrc;
      return this;
    }

    public String getSrcId() {
      return srcId;
    }

    public String getServiceId() {
      return serviceId;
    }

    public String getMsgSrc() {
      return msgSrc;
    }

    @Override
    public String toString() {
      return "Cmpp20{" +
          "spNumber='" + getSpNumber() + '\'' +
          ", phones=" + Arrays.toString(getPhones()) +
          ", pdu=" + getPdu() +
          ", srcId='" + srcId + '\'' +
          ", serviceId='" + serviceId + '\'' +
          ", msgSrc='" + msgSrc + '\'' +
          '}';
    }
  }

  public static class Sgip12 extends Message {

    private String corpId;
    private String serviceType;
    private int morelatetoMTFlag = 3;

    public Sgip12 corpId(String corpId) {
      this.corpId = corpId;
      return this;
    }

    public Sgip12 serviceType(String serviceType) {
      this.serviceType = serviceType;
      return this;
    }

    public Sgip12 morelatetoMTFlag(int morelatetoMTFlag) {
      this.morelatetoMTFlag = morelatetoMTFlag;
      return this;
    }

    public String getCorpId() {
      return corpId;
    }

    public String getServiceType() {
      return serviceType;
    }

    public int getMorelatetoMTFlag() {
      return morelatetoMTFlag;
    }

    @Override
    public String toString() {
      return "Sgip12{" +
          "spNumber='" + getSpNumber() + '\'' +
          ", phones=" + Arrays.toString(getPhones()) +
          ", pdu=" + getPdu() +
          ", corpId='" + corpId + '\'' +
          ", serviceType='" + serviceType + '\'' +
          ", morelatetoMTFlag=" + morelatetoMTFlag +
          '}';
    }
  }
}
