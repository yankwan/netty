package io.netty.example.mytest.reactor;

import io.netty.example.mytest.nio.CodecUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MutilThreadHandler implements Runnable {
    private final SocketChannel socket;
    private final SelectionKey sk;
    private final Selector selector;

    private String subReactorName;

    ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());


    MutilThreadHandler(Selector selector, SocketChannel channel) throws IOException {
        this(selector, channel, null);
    }

    MutilThreadHandler(Selector selector, SocketChannel channel, String subReactorName) throws IOException {
        this.subReactorName = subReactorName;
        this.selector = selector;
        this.socket = channel;
        channel.configureBlocking(false);
        sk = socket.register(selector, SelectionKey.OP_READ);
        // 将Handler实例(自己)添加到attachment中
        // 该channel的附加对象，即客户端channel
        sk.attach(this);
    }

    @Override
    public void run() {
        try {
            if (sk.isReadable()) {
                read();
            } else if (sk.isWritable()) {
                send();
            } else {
                // TO DO
            }
        } catch (IOException e) {

        }
    }

    private synchronized void read() throws IOException {

        final ByteBuffer input = CodecUtil.read(socket);
        if (input == null) {
            System.out.println("断开 Channel");
            socket.register(selector, 0);
            return;
        }

        if (input.position() > 0) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    process(input);
                }
            });
            // 下一步处理写事件
            sk.interestOps(SelectionKey.OP_WRITE);
        }
    }

    private synchronized  void send() throws IOException {

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                String content = "hello world_" + socket.socket().getRemoteSocketAddress();
                System.out.println("SEND DATA：" + content);
                CodecUtil.write(socket, content);
            }
        });
        sk.interestOps(SelectionKey.OP_READ);
    }

    private void process(ByteBuffer byteBuffer) {
        String content = CodecUtil.newString(byteBuffer);
        System.out.println(this.subReactorName + " " + Thread.currentThread().getName() + " 读取数据：" + content);
    }

}
