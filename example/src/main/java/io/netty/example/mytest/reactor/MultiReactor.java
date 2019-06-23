package io.netty.example.mytest.reactor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class MultiReactor implements Runnable {

    final ServerSocketChannel serverSocket;
    Selector[] selectors = new Selector[Runtime.getRuntime().availableProcessors()];
    int next = 0;

    MultiReactor(int port) throws IOException {
        init();
        serverSocket = ServerSocketChannel.open();
        serverSocket.socket().bind(
                new InetSocketAddress(port)
        );
        serverSocket.configureBlocking(false);
        // 首先注册感兴趣事件，连接事件
        SelectionKey sk = serverSocket.register(selectors[0], SelectionKey.OP_ACCEPT);
        System.out.println("Server 启动完成");

        // Acceptor类处理新的连接
        sk.attach(new MultiReactor.Acceptor());
    }

    private void init() throws IOException {
        for (int i = 0; i < selectors.length; i++) {
            selectors[i] = Selector.open();
        }
    }


    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                for (int i = 0; i < selectors.length; i++) {
                    selectors[i].select();
                    Set selected = selectors[i].selectedKeys();
                    Iterator it = selected.iterator();
                    while (it.hasNext()) {
                        dispatch((SelectionKey) it.next());
                    }
                    selected.clear();
                }
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
        public synchronized void run() {
            try {
                SocketChannel c = serverSocket.accept();
                if (c != null) {
                    // 注册读写
                    new MutilThreadHandler(selectors[next], c);
                }

                if (++next == selectors.length) next = 0;
            } catch (Exception e) {
                // TO DO
            }
        }
    }

    public static void main(String[] args) throws IOException {
        MultiReactor reactor = new MultiReactor(8080);
        Thread t = new Thread(reactor);
        t.start();
    }

}
