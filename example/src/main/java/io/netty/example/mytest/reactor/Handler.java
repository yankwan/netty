package io.netty.example.mytest.reactor;


import io.netty.example.mytest.nio.CodecUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

public class Handler implements Runnable {

    final SocketChannel socket;
    final SelectionKey sk;
    final Selector selector;

//    ByteBuffer input = ByteBuffer.allocate(1024);
//    ByteBuffer output = ByteBuffer.allocate(1024);

    static final int READING = 0, SENDING = 1;
    int state = READING;

    Handler(Selector sel, SocketChannel c) throws IOException {
        selector = sel;
        socket = c;
        c.configureBlocking(false);
        sk = socket.register(sel, 0);
        // 将Handler实例(自己)添加到attachment中
        sk.attach(this);
        // 服务端注册读事件，读取客户端信息
        sk.interestOps(SelectionKey.OP_READ);
        // 唤醒阻塞在selector.select()上的线程
        selector.wakeup();
    }

    private boolean inputIsComplete() {
        return true;
    }

    private boolean outputIsComplete() {
        return true;
    }

    private void process(ByteBuffer byteBuffer) {
        String content = CodecUtil.newString(byteBuffer);
        System.out.println("读取数据：" + content);
    }


    @Override
    public void run() {
        try {
            if (state == READING) read();
            else if (state == SENDING) send();
        } catch (IOException e) {
            // TO DO
        }
    }

    private void read() throws IOException {

        ByteBuffer input = CodecUtil.read(socket);
        if (input == null) {
            System.out.println("断开 Channel");
            socket.register(selector, 0);
            return;
        }

        if (input.position() > 0) {
            process(input);
            state = SENDING;
            // 下一步处理写事件
            sk.interestOps(SelectionKey.OP_WRITE);
        }
    }

    private void send() throws IOException {
        String content = "hello world";
        System.out.println("写入数据：" + content);
        CodecUtil.write(socket, content);
        state = READING;
        sk.interestOps(SelectionKey.OP_READ);
    }
}
