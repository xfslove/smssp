# smssp

短彩信接口机，开箱即用

可对接联通（SGIP1.2）、移动（CMPP2.0）SP Server



#### maven

```
    <repositories>
        <repository>
            <id>oss.sonatype.org-snapshot</id>
            <url>http://oss.sonatype.org/content/repositories/snapshots</url>
            <releases>
                <enabled>false</enabled>
            </releases>
            <snapshots>
                <enabled>true</enabled>
            </snapshots>
        </repository>
    </repositories>
    
    <dependency>
        <groupId>com.github.xfslove</groupId>
        <artifactId>smssp</artifactId>
        <version>1.0.2-SNAPSHOT</version>
    </dependency>
```





### CMPP2.0

#### Handle Submit Messages

```
    Cmpp20Client client = Cmpp20Client
        .newConnection(nodeId, "username", "password", "host", port)
        .connections(maxConnections);

    // mms
    Message mms = Message.cmpp20()
        .serviceId("serviceId")
        .msgSrc("msgSrc")
        .spNumber("spNumber")
        .phones("phone1", "phone2")
        .mms()
        .contentLocation("contentLocation")
        .transactionId("transactionId")
        .from("from")
        .size(size)
        .end();

    // sms
    Message sms = Message.cmpp20()
        .serviceId("serviceId")
        .msgSrc("msgSrc")
        .spNumber("spNumber")
        .phones("phone1", "phone2")
        .sms()
        .text("text")
        .end();

    SubmitMessage[] toSubmitMessages = client.convert(sms); 
                                    // client.convert(mms);

    // sync
    client.connect();
    for (SubmitMessage toSubmitMessage : toSubmitMessages) {
      SubmitRespMessage submitRespMessage = client.submit(toSubmitMessage, timeout);
      // do something with resp
    }

    // async
    client.responseListener(new ResponseListener() {
      @Override
      public void done(Response response) {

        SubmitRespMessage submitRespMessage = (SubmitRespMessage) response;
        // do something with resp
      }
    }).connect();
    for (SubmitMessage toSubmitMessage : toSubmitMessages) {
      client.submit(toSubmitMessage);
    }
```

#### Handle Deliver Messages

```
    client.notificationListener(new DefaultProxyListener("username", new CustomListener()));

    // deliver messages
    class CustomListener extends Cmpp20AdaptListener {

      @Override
      protected void done(DeliverMessage deliverMessage) {
        // do something with deliver message
      }

      @Override
      protected void done(DeliverMessage.Report report) {
        // do something with report message
      }
    }
```

### SGIP1.2

#### Handle Submit Messages

```
    Sgip12Client client = Sgip12Client
        .newConnection(nodeId, "username", "password", "host", port)
        .connections(maxConnections);

    // mms
    Message mms = Message.sgip12()
        .corpId("corpId")
        .serviceType("serviceType")
        .morelatetoMTFlag(morelatetoMTFlag)
        .spNumber("spNumber")
        .phones("phone1", "phone2")
        .mms()
        .contentLocation("contentLocation")
        .transactionId("transactionId")
        .from("from")
        .size(size)
        .end();

    // sms
    Message sms = Message.sgip12()
        .corpId("corpId")
        .serviceType("serviceType")
        .morelatetoMTFlag(morelatetoMTFlag)
        .spNumber("spNumber")
        .phones("phone1", "phone2")
        .sms()
        .text("text")
        .end();

    SubmitMessage[] toSubmitMessages = client.convert(sms); 
                                    // client.convert(mms);

    // sync
    client.connect();
    for (SubmitMessage toSubmitMessage : toSubmitMessages) {
      SubmitRespMessage submitRespMessage = client.submit(toSubmitMessage, timeout);
      // do something with resp
    }

    // async
    client.responseListener(new ResponseListener() {
      @Override
      public void done(Response response) {

        SubmitRespMessage submitRespMessage = (SubmitRespMessage) response;
        // do something with resp
      }
    }).connect();
    for (SubmitMessage toSubmitMessage : toSubmitMessages) {
      client.submit(toSubmitMessage);
    }
```

#### Handle Deliver Message

```
    Sgip12Server server = Sgip12Server.newBind(nodeId, "username", "password", port);

    // deliver messages
    class CustomListener extends Sgip12AdaptListener {

      @Override
      protected void done(DeliverMessage deliverMessage) {
        // do something with deliver message
      }

      @Override
      protected void done(ReportMessage reportMessage) {
        // do something with report message
      }

    }

    server.notificationListener(new DefaultProxyListener("username", new CustomListener()));
    
    server.bind();
```
