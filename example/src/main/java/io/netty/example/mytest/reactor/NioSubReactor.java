package io.netty.example.mytest.reactor;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class NioSubReactor implements Runnable {

    private Selector selector;
    private String subReactorName;

    public NioSubReactor(String name) throws IOException {
        this.subReactorName = name;
        selector = Selector.open();
    }

    public void register(SocketChannel socketChannel) throws IOException {
        socketChannel.register(selector, SelectionKey.OP_READ);
        new MutilThreadHandler(selector, socketChannel, this.subReactorName);
    }

    public void wakeup() {
        // 唤醒因为selector.select()阻塞的方法
        selector.wakeup();
    }

    @Override
    public void run() {
        while(true) {
            try {
                selector.select();
                Set selectionKeys = selector.selectedKeys();
                Iterator it = selectionKeys.iterator();
                while (it.hasNext()) {
                    // 已准备就绪，对事件进行分发
                    // 已准备就绪的事件不用再考虑IO是否已经好了
                    dispatch((SelectionKey) it.next());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void dispatch(SelectionKey k) {
        // 从attachment中获取Handler
        Runnable r = (Runnable) (k.attachment());
        if (r != null)
            r.run();
    }

}
