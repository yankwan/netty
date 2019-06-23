package io.netty.example.mytest.reactor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class NioMainReactor implements Runnable {

    AtomicInteger incr = new AtomicInteger(0);

    private Selector selector;
    private ServerSocketChannel serverSocketChannel;
    private NioSubReactor[] subReactors;
    int coreNum;

    public NioMainReactor(int port) throws IOException {
        selector = Selector.open();
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        serverSocketChannel.socket().bind(new InetSocketAddress(port));
        // 当前cpu数量
        coreNum = Runtime.getRuntime().availableProcessors();
        subReactors = new NioSubReactor[coreNum];

        for (int i = 0; i < subReactors.length; i++) {
            subReactors[i] = new NioSubReactor("SubReactor-" + i);
        }

        System.out.println("Server端已启动...");
    }

    @Override
    public void run() {
        while(true) {
            try {
                selector.select();
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator it = selectionKeys.iterator();
                if (it.hasNext()) {
                    SelectionKey key = (SelectionKey) it.next();
                    it.remove();
                    if (key.isAcceptable()) {
                        SocketChannel socketChannel = ((ServerSocketChannel) key.channel()).accept();
                        new Acceptor(socketChannel);
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class Acceptor {
        public Acceptor(SocketChannel socketChannel) throws IOException {
            socketChannel.configureBlocking(false);
            int index = incr.getAndIncrement() % subReactors.length;
            NioSubReactor subReactor = subReactors[index];
            subReactor.register(socketChannel);
            new Thread(subReactor).start();
            System.out.println("收到新连接：" + socketChannel);
        }
    }

    public static void main(String[] args) throws IOException {
        NioMainReactor reactor = new NioMainReactor(8080);
        Thread t = new Thread(reactor);
        t.start();
    }
}
