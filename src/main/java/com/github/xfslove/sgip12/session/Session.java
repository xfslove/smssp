package com.github.xfslove.sgip12.session;

import io.netty.handler.codec.http.HttpClientCodec;
import reactor.ipc.netty.NettyPipeline;
import reactor.ipc.netty.options.ServerOptions;
import reactor.ipc.netty.resources.LoopResources;
import reactor.ipc.netty.tcp.TcpClient;
import reactor.ipc.netty.tcp.TcpServer;

/**
 * @author hanwen
 * created at 2018/8/28
 */
public class Session {

  public static void main(String[] args) {

    ServerOptions options = ServerOptions.builder().loopResources(LoopResources.create("test", 1, 4, true)).build();

    TcpServer.builder().port(8888).build()
        .start((in, out) -> {
          return out
              .options(NettyPipeline.SendOptions::flushOnEach)
              .sendString(
                  in.receive()
                      .asString()
                      .map(s -> "ECHO: " + s)
              )
              .neverComplete();
        });

  }
}
