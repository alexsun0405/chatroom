package com.ludi.echo;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class Client {
    public static final String LOCAL_HOST = "localhost";
    public static final int DEFAULT_PORT = 8888;
    AsynchronousSocketChannel asynchronousSocketChannel;

    private void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void start() {
        try {
            asynchronousSocketChannel = AsynchronousSocketChannel.open();
            Future<Void> future = asynchronousSocketChannel.connect(new InetSocketAddress(LOCAL_HOST, DEFAULT_PORT));
            future.get();

            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                String input = consoleReader.readLine();

                ByteBuffer buffer = ByteBuffer.wrap(input.getBytes());
                Future<Integer> writeFuture = asynchronousSocketChannel.write(buffer);
                writeFuture.get();
                buffer.flip();
                Future<Integer> readFuture = asynchronousSocketChannel.read(buffer);
                readFuture.get();
                String echo = new String(buffer.array());
                System.out.println(echo);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Client client = new Client();
        client.start();
    }
}
