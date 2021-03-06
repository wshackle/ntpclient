package com.github.wshackle.ntpclient;

/*
This is a modified version of example by Jason Mathews, MITRE Corp that was 
published on https://commons.apache.org/proper/commons-net/index.html 
with the Apache Commons Net software.
 */
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.text.NumberFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.NtpUtils;
import org.apache.commons.net.ntp.NtpV3Packet;
import org.apache.commons.net.ntp.TimeInfo;
import org.apache.commons.net.ntp.TimeStamp;

/**
 * NTPClient polls an NTP server with UDP  and returns milli seconds with
 * currentTimeMillis() intended as drop in replacement for System.currentTimeMillis()
 * 
 * @author Will Shackleford
 */
public final class NTPClient implements AutoCloseable {

//    private static final NumberFormat numberFormat = new java.text.DecimalFormat("0.00");
//
//    /**
//     * Process <code>TimeInfo</code> object and print its details.
//     *
//     * @param info <code>TimeInfo</code> object.
//     */
//    public static void processResponse(TimeInfo info) {
//        NtpV3Packet message = info.getMessage();
//        int stratum = message.getStratum();
//        String refType;
//        if (stratum <= 0) {
//            refType = "(Unspecified or Unavailable)";
//        } else if (stratum == 1) {
//            refType = "(Primary Reference; e.g., GPS)"; // GPS, radio clock, etc.
//        } else {
//            refType = "(Secondary Reference; e.g. via NTP or SNTP)";
//        }
//        // stratum should be 0..15...
//        System.out.println(" Stratum: " + stratum + " " + refType);
//        int version = message.getVersion();
//        int li = message.getLeapIndicator();
//        System.out.println(" leap=" + li + ", version="
//                + version + ", precision=" + message.getPrecision());
//
//        System.out.println(" mode: " + message.getModeName() + " (" + message.getMode() + ")");
//        int poll = message.getPoll();
//        // poll value typically btwn MINPOLL (4) and MAXPOLL (14)
//        System.out.println(" poll: " + (poll <= 0 ? 1 : (int) Math.pow(2, poll))
//                + " seconds" + " (2 ** " + poll + ")");
//        double disp = message.getRootDispersionInMillisDouble();
//        System.out.println(" rootdelay=" + numberFormat.format(message.getRootDelayInMillisDouble())
//                + ", rootdispersion(ms): " + numberFormat.format(disp));
//
//        int refId = message.getReferenceId();
//        String refAddr = NtpUtils.getHostAddress(refId);
//        String refName = null;
//        if (refId != 0) {
//            if (refAddr.equals("127.127.1.0")) {
//                refName = "LOCAL"; // This is the ref address for the Local Clock
//            } else if (stratum >= 2) {
//                // If reference id has 127.127 prefix then it uses its own reference clock
//                // defined in the form 127.127.clock-type.unit-num (e.g. 127.127.8.0 mode 5
//                // for GENERIC DCF77 AM; see refclock.htm from the NTP software distribution.
//                if (!refAddr.startsWith("127.127")) {
//                    try {
//                        InetAddress addr = InetAddress.getByName(refAddr);
//                        String name = addr.getHostName();
//                        if (name != null && !name.equals(refAddr)) {
//                            refName = name;
//                        }
//                    } catch (UnknownHostException e) {
//                        // some stratum-2 servers sync to ref clock device but fudge stratum level higher... (e.g. 2)
//                        // ref not valid host maybe it's a reference clock name?
//                        // otherwise just show the ref IP address.
//                        refName = NtpUtils.getReferenceClock(message);
//                    }
//                }
//            } else if (version >= 3 && (stratum == 0 || stratum == 1)) {
//                refName = NtpUtils.getReferenceClock(message);
//                // refname usually have at least 3 characters (e.g. GPS, WWV, LCL, etc.)
//            }
//            // otherwise give up on naming the beast...
//        }
//        if (refName != null && refName.length() > 1) {
//            refAddr += " (" + refName + ")";
//        }
//        System.out.println(" Reference Identifier:\t" + refAddr);
//
//        TimeStamp refNtpTime = message.getReferenceTimeStamp();
//        System.out.println(" Reference Timestamp:\t" + refNtpTime + "  " + refNtpTime.toDateString());
//
//        // Originate Time is time request sent by client (t1)
//        TimeStamp origNtpTime = message.getOriginateTimeStamp();
//        System.out.println(" Originate Timestamp:\t" + origNtpTime + "  " + origNtpTime.toDateString());
//
//        long destTime = info.getReturnTime();
//        // Receive Time is time request received by server (t2)
//        TimeStamp rcvNtpTime = message.getReceiveTimeStamp();
//        System.out.println(" Receive Timestamp:\t" + rcvNtpTime + "  " + rcvNtpTime.toDateString());
//
//        // Transmit time is time reply sent by server (t3)
//        TimeStamp xmitNtpTime = message.getTransmitTimeStamp();
//        System.out.println(" Transmit Timestamp:\t" + xmitNtpTime + "  " + xmitNtpTime.toDateString());
//
//        // Destination time is time reply received by client (t4)
//        TimeStamp destNtpTime = TimeStamp.getNtpTime(destTime);
//        System.out.println(" Destination Timestamp:\t" + destNtpTime + "  " + destNtpTime.toDateString());
//
//        info.computeDetails(); // compute offset/delay if not already done
//        Long offsetValue = info.getOffset();
//        Long delayValue = info.getDelay();
//        String delay = (delayValue == null) ? "N/A" : delayValue.toString();
//        String offset = (offsetValue == null) ? "N/A" : offsetValue.toString();
//
//        System.out.println(" Roundtrip delay(ms)=" + delay
//                + ", clock offset(ms)=" + offset); // offset in ms
//    }

    final InetAddress hostAddr;
    NTPUDPClient ntpUdpClient;
    Thread pollThread = null;
    final long poll_ms;

    private void pollNtpServer() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(poll_ms);
                    TimeInfo ti = ntpUdpClient.getTime(hostAddr);
//                    long diff0 = ti.getMessage().getReceiveTimeStamp().getTime() - System.currentTimeMillis();
//                    System.out.println("diff0 = " + diff0);
                    this.setTimeInfo(ti);
                } catch (SocketTimeoutException ste) {
                }
            }
        } catch (InterruptedException interruptedException) {
        } catch (IOException ex) {
            Logger.getLogger(NTPClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Connect to host and poll the host every poll_ms milliseconds. 
     * Thread is started in the constructor.
     * @param host
     * @param poll_ms
     * @throws UnknownHostException
     * @throws SocketException
     */
    public NTPClient(String host, int poll_ms) throws UnknownHostException, SocketException {
        this.poll_ms = poll_ms;
        hostAddr = InetAddress.getByName(host);
        ntpUdpClient = new NTPUDPClient();
        ntpUdpClient.setDefaultTimeout(10000);
        ntpUdpClient.open();
        ntpUdpClient.setSoTimeout(poll_ms * 2 + 20);
        pollThread = new Thread(this::pollNtpServer, "pollNtpServer(" + host + "," + poll_ms + ")");
        pollThread.start();
    }

    private TimeInfo timeInfo;
    private long timeInfoSetLocalTime;

    /**
     * Get the value of timeInfo
     *
     * @return the value of timeInfo
     */
    public synchronized TimeInfo getTimeInfo() {
        return timeInfo;
    }

    private synchronized void setTimeInfo(TimeInfo timeInfo) {
        this.timeInfo = timeInfo;
        timeInfoSetLocalTime = System.currentTimeMillis();
    }

    /**
     * Returns milliseconds just as System.currentTimeMillis() but using the latest
     * estimate from the remote time server.
     * @return the difference, measured in milliseconds, between the current time and midnight, January 1, 1970 UTC.
     */
    public long currentTimeMillis() {
        long diff = System.currentTimeMillis() - timeInfoSetLocalTime;
//        System.out.println("diff = " + diff);
        return timeInfo.getMessage().getReceiveTimeStamp().getTime() + diff;
    }

    /**
     * Polls an NTP server printing the current Date as recieved from it an the difference 
     * between that and System.currentTimeMillis()
     * @param args host name of ntp server in first element
     * @throws UnknownHostException
     * @throws SocketException
     * @throws InterruptedException
     * @throws IOException
     * @throws Exception
     */
    public static void main(String[] args) throws UnknownHostException, SocketException, InterruptedException, IOException, Exception {
        if (args.length < 1) {
            args = new String[]{"time-a.nist.gov"};
        }

        try (NTPClient ntp = new NTPClient(args[0], 100)) {

            for (int i = 0; i < 10; i++) {
                Thread.sleep(1000);
                long t1 = System.currentTimeMillis();
                long t2 = ntp.currentTimeMillis();
                long t3 = System.currentTimeMillis();

                Date d = new Date(t2);
                System.out.println(d + " :  diff = " + (t3 - t2) + " ms");
            }
        }
//        if (args.length == 0) {
//            args = new String[]{"time.nist.gov"};
////            System.err.println("Usage: NTPClient <hostname-or-address-list>");
////            System.exit(1);
//        }
//
//        NTPUDPClient client = new NTPUDPClient();
//        // We want to timeout if a response takes longer than 10 seconds
//        client.setDefaultTimeout(10000);
//        InetAddress hostAddr = InetAddress.getByName("time-b.nist.gov");
//        client.getTime(hostAddr);
//        try {
//            client.open();
//            for (String arg : args)
//            {
//                System.out.println();
//                try {
//                    InetAddress hostAddr = InetAddress.getByName(arg);
//                    System.out.println("> " + hostAddr.getHostName() + "/" + hostAddr.getHostAddress());
//                    TimeInfo info = client.getTime(hostAddr);
////                    processResponse(info);
//                } catch (IOException ioe) {
//                    ioe.printStackTrace();
//                }
//            }
//        } catch (SocketException e) {
//            e.printStackTrace();
//        }
//
//        client.close();
    }

    private boolean closed = false;

    @Override
    public void close() throws Exception {
        if (null != pollThread) {
            pollThread.interrupt();
            pollThread.join(200);
            pollThread = null;
        }
        if (null != ntpUdpClient) {
            ntpUdpClient.close();
            ntpUdpClient = null;
        }

    }

    protected void finalizer() {
        try {
            this.close();
        } catch (Exception ex) {
            Logger.getLogger(NTPClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
