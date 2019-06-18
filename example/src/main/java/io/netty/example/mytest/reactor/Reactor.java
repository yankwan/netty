package io.netty.example.mytest.reactor;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class Reactor implements Runnable {

    final Selector selector;
    final ServerSocketChannel serverSocket;

    Reactor(int port) throws IOException {
        selector = Selector.open();
        serverSocket = ServerSocketChannel.open();
        serverSocket.socket().bind(
                new InetSocketAddress(port)
        );
        serverSocket.configureBlocking(false);
        // 首先注册感兴趣事件，连接事件
        SelectionKey sk = serverSocket.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("Server 启动完成");

        // Acceptor类处理新的连接
        sk.attach(new Acceptor());
    }


    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                // 阻塞等待事件  没有准备就绪的事件将会阻塞
                selector.select();
                Set selected = selector.selectedKeys();
                Iterator it = selected.iterator();
                while (it.hasNext())
                    // 已准备就绪，对事件进行分发
                    dispatch((SelectionKey) it.next());
                selected.clear();
            }
        } catch (IOException e) {
            // TO DO
        }
    }

    private void dispatch(SelectionKey k) {
        // 从attachment中获取Handler或Acceptor
        Runnable r = (Runnable) (k.attachment());
        if (r != null)
            r.run();
    }


    /**
     * inner class
     * 处理客户端连接
     */
    class Acceptor implements Runnable {

        @Override
        public void run() {
            try {
                SocketChannel c = serverSocket.accept();
                if (c != null)
                    // 注册读写
                    new Handler(selector, c);
            } catch (IOException e) {
                // TO DO
            }
        }
    }


    public static void main(String[] args) throws IOException {
        Reactor reactor = new Reactor(8080);
        Thread t = new Thread(reactor);
        t.start();
    }
}
