package de.fabmax.binparse;

import de.fabmax.binparse.examples.DnsMessage;

import java.io.ByteArrayInputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

/**
 * Created by max on 15.11.2015.
 */
public class ServiceDiscovery {

    public static void main(String[] args) throws Exception {
        //test();
        testLive();
        //testSimple();
    }

    public static void testLive() throws Exception {
        /*Enumeration<NetworkInterface> nifs = NetworkInterface.getNetworkInterfaces();
        for (NetworkInterface nif : Collections.list(nifs)) {
            System.out.println(nif.getDisplayName());
            Enumeration<InetAddress> addrs = nif.getInetAddresses();
            for (InetAddress addr : Collections.list(addrs)) {
                System.out.println("  " + addr);
            }
        }*/

        InetAddress group = InetAddress.getByName("224.0.0.251");

        MulticastSocket sock = new MulticastSocket(5353);
        sock.joinGroup(group);
        System.out.println("created socket");

        DatagramPacket packet = new DatagramPacket(new byte[1500], 1500);

        Parser parser = Parser.Companion.fromFile("src/test/binparse/dns-sd.bp");
        Struct main = parser.getStructs().get("main");

        while (true) {
            sock.receive(packet);
            //System.out.println("\n\nreceived a packet: " + packet.getLength() + " bytes");
            //System.out.println(new String(packet.getData(), 0, packet.getLength()));

            try {
                ByteArrayInputStream bin = new ByteArrayInputStream(packet.getData());
                ParseResult result = main.parse(bin);
                new DnsMessage(result, packet.getAddress());
                //System.out.println(result.toString(0, true));

            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("{ ");
                for (int i = 1; i <= packet.getLength(); i++) {
                    System.out.printf("(byte) %d, ", ((int) packet.getData()[i-1]) & 0xff);
                    if (i % 8 == 0) {
                        System.out.println();
                    }
                }
                System.out.println("}");
                break;
            }

            /**/
        }

    }

    private static void test() throws Exception {
        byte[] buf = new byte[] {
                (byte) 0x00, (byte) 0x00, (byte) 0x84, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x04,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0a, (byte) 0x66, (byte) 0x61, (byte) 0x72,
                (byte) 0x62, (byte) 0x72, (byte) 0x61, (byte) 0x75, (byte) 0x73, (byte) 0x63, (byte) 0x68, (byte) 0x03,
                (byte) 0x5f, (byte) 0x66, (byte) 0x72, (byte) 0x04, (byte) 0x5f, (byte) 0x74, (byte) 0x63, (byte) 0x70,
                (byte) 0x05, (byte) 0x6c, (byte) 0x6f, (byte) 0x63, (byte) 0x61, (byte) 0x6c, (byte) 0x00, (byte) 0x00,
                (byte) 0x10, (byte) 0x80, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x0e, (byte) 0x0f, (byte) 0x00,
                (byte) 0x01, (byte) 0x00, (byte) 0xc0, (byte) 0x0c, (byte) 0x00, (byte) 0x21, (byte) 0x80, (byte) 0x01,
                (byte) 0x00, (byte) 0x00, (byte) 0x0e, (byte) 0x0f, (byte) 0x00, (byte) 0x16, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x75, (byte) 0xfc, (byte) 0x0d, (byte) 0x31, (byte) 0x39, (byte) 0x32,
                (byte) 0x2d, (byte) 0x31, (byte) 0x36, (byte) 0x38, (byte) 0x2d, (byte) 0x31, (byte) 0x30, (byte) 0x2d,
                (byte) 0x37, (byte) 0x32, (byte) 0xc0, (byte) 0x20, (byte) 0xc0, (byte) 0x44, (byte) 0x00, (byte) 0x01,
                (byte) 0x80, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x0e, (byte) 0x0f, (byte) 0x00, (byte) 0x04,
                (byte) 0xc0, (byte) 0xa8, (byte) 0x0a, (byte) 0x48, (byte) 0xc0, (byte) 0x17, (byte) 0x00, (byte) 0x0c,
                (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x0e, (byte) 0x0f, (byte) 0x00, (byte) 0x02,
                (byte) 0xc0, (byte) 0x0c, };

        Parser parser = Parser.Companion.fromFile("src/test/binparse/dns-sd.bp");
        Struct main = parser.getStructs().get("main");

        /*
        ParseResult result = null;
        for (int i = 0; i < 10000; i++) {
            ByteArrayInputStream bin = new ByteArrayInputStream(buf);
            result = main.parse(bin);
        }

        long t = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            ByteArrayInputStream bin = new ByteArrayInputStream(buf);
            result = main.parse(bin);
        }
        t = System.nanoTime() - t;
        System.out.printf("parsing took %.3f ms\n", t / 100e6);*/


        ByteArrayInputStream bin = new ByteArrayInputStream(buf);
        ParseResult result = main.parse(bin);
        new DnsMessage(result, InetAddress.getLocalHost());

        /*Iterable<Field> flaterator = result::flat;
        Stream<Field> fields = StreamSupport.stream(flaterator.spliterator(), false);
        fields.filter(it -> it instanceof StringField).forEach(it -> System.out.println(it.getOffset() + ": " + it.getStringValue()));*/


        //System.out.println(result.toString(0, true));
        /*ArrayField answers = result.getArray("answers");
        for (int i = 0; i < answers.getLength(); i++) {
            ParseResult ans = answers.getSruct(i);
            if (ans.getInt("type") == 33) {
                ParseResult service = ans.getStruct("data");
                int port = service.getInt("port");
                String ip = service.getArray("target").getSruct(0).getString("value.text");
                System.out.println("found a service: " + ip + ":" + port);
            }
        }*/
    }

    private static void testSimple() throws Exception {
        byte[] buf = new byte[] { (byte) 0xc0, 8 };

        Parser parser = Parser.Companion.fromFile("test.sbp");
        Struct main = parser.getStructs().get("main");
        ByteArrayInputStream bin = new ByteArrayInputStream(buf);
        ParseResult result = main.parse(bin);

        Field value = result.get("val");
        System.out.println(value.getClass());
        System.out.println(value.getQualifiers());
        System.out.println(value.hasQualifier("COLLECT"));
    }
}
