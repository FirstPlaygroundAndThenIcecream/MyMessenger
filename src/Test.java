import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

public class Test
{
    private static BufferedReader in;
    private static PrintWriter out;

    public static void main(String[] args)
    {
        UserTest Lei = new UserTest("Lei", "localhost", 6789);
        Socket LeiSocket = Lei.getSocket();

        System.out.println(LeiSocket.getLocalPort());
//        Lei.closeSocket();

        System.out.println(Lei.getName());

        try
        {
            in = new BufferedReader(new InputStreamReader(LeiSocket.getInputStream()));
            out = new PrintWriter(LeiSocket.getOutputStream(), true);
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        int i = 0;
        do
        {
            try
            {
                String serverSays = in.readLine();
                System.out.println(serverSays);
                i++;

                out.println("Client has received: " + serverSays);
                out.flush();
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }while(i < 10);


    }
}



class UserTest
{
    String name;
    InetAddress inetAddress;
    int port;
    boolean online;
    Socket socket;

    UserTest(String name, String ipAddress, int port)
    {
        this.name = name;
        try
        {
            this.socket = new Socket(ipAddress, port);
            this.inetAddress = InetAddress.getByName(ipAddress);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        this.port = port;
    }

    Socket getSocket()
    {
        return socket;
    }

    String getName()
    {
        return name;
    }

    boolean isOnline()
    {
        return online;
    }

    public void setOnline(boolean online)
    {
        this.online = online;
    }

    void closeSocket()
    {
        try
        {
            this.socket.close();
            System.out.println("clientSocket is closed");
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
