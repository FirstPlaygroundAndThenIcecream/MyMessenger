import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;
import java.util.regex.Pattern;

public class Client
{
    private static Socket clientSocket;
    private static String userName;
    private static BufferedReader readFromServer;
    private static BufferedWriter writeToServer;


    public static void main(String[] args)
    {
        String serverReplyMsg = connectServer();

        if(serverReplyMsg.equals("J_OK"))
        {
            System.out.println(serverReplyMsg + "\n");

            Receiver receiver = new Receiver(clientSocket);
            receiver.start();

            Sender sender = new Sender(clientSocket, userName);
            sender.start();
        }
        if(serverReplyMsg.equals("J_ER"))
        {
            System.out.println(serverReplyMsg + ": name exists, please restart and choose a new name." );
            System.exit(1);
        }
//        save chat content inputFromClient a file
    }

    //collect information(username/ip/port) from console input of user
    private static String connectServer()
    {
        Scanner console = new Scanner(System.in);

        System.out.println("User name: ");
        boolean namePass;

        do
        {
            userName = console.nextLine();
            namePass = checkUserNameValidation(userName.trim());
            if(!namePass)
                System.out.println("-> Invalidate name, type again: ");
        }while(!namePass);

        String userInfoToServer;
        String serverReply = "";
        try
        {
            System.out.println("Ip: ");
            InetAddress ipAddress = InetAddress.getByName(console.nextLine());

            System.out.println("Port: ");
            int port = console.nextInt();

            System.out.println("-> connecting server...\n");

            clientSocket = new Socket(ipAddress.getHostAddress(), port);
            userInfoToServer = userName + ":" + ipAddress.getHostAddress() + ":" + port;
            serverReply = sendJoinCommand(userInfoToServer);
            System.out.println("-> " + userInfoToServer);

        } catch (Exception e)
        {
            System.out.println("-> Unknown host or port");
            System.exit(1);
        }

        return serverReply;
    }

    //send JOIN command to server and get reply from server
    private static String sendJoinCommand(String userInput)
    {
        try
        {
            readFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            writeToServer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        String[] userInfo = userInput.split(":");

        //JOIN <<user_name>>, <<server_ip>>:<<server_port>>
        String requestJOIN = "JOIN " + userInfo[0] + ", " + userInfo[1] + ":" + userInfo[2];
        try
        {
            writeToServer.write(requestJOIN + "\n");
            writeToServer.flush();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        System.out.println("-> " + requestJOIN + "\n");

        try
        {
            String serverReply = readFromServer.readLine();
            return serverReply;
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        return " -> server no reply";
    }

    //Username is max 12 chars long, only letters, digits, ‘-‘ and ‘_’ allowed
    //at this stage, only check by the client side if the user name fits the requirement
    private static boolean checkUserNameValidation(String userName)
    {
        Pattern pattern = Pattern.compile("[A-Za-z0-9_-]+");
        return userName!= null && userName.length() <= 12 && pattern.matcher(userName).matches();
    }
}

class Sender extends Thread
{
    Socket socket;
    BufferedWriter outputToServer;
    Scanner console;
    String userName;

    Sender(Socket socket, String userName)
    {
        this.socket = socket;
        this.userName = userName;
        this.console = new Scanner(System.in);
        try
        {
            this.outputToServer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void run()
    {
        String command_QUIT;

        command_QUIT = sendDataCommand();
        sendQuitCommand(command_QUIT);

        try
        {
            socket.close();
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        System.exit(0);
    }

    private String sendDataCommand()
    {
        String userMsg;
        sendImavCommand();

        do
        {
            do{
                System.out.println();
                System.out.println("** " + userName + ": ");
                userMsg = console.nextLine();
                if(userMsg.length() > 250)
                {
                    System.out.println("\nmessage too long, type again");
                }
            } while (userMsg.length() > 250);

            //send DATA command
            //DATA <<user_name>>: <<free text...>>
            if(!userMsg.trim().equalsIgnoreCase("quit"))
            {
                try
                {
                    outputToServer.write("DATA " + userName + ": " + userMsg + "\n");
                    outputToServer.flush();
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }while(!userMsg.trim().equalsIgnoreCase("quit"));
        return userMsg;
    }

    private void sendImavCommand()
    {
        Thread imavThread = new Thread(() ->
        {
//            BufferedWriter sendImav = null;
            try
            {
                 outputToServer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                while (true)
                {
                    outputToServer.write("IMAV\n");
                    outputToServer.flush();
                    Thread.sleep(60000);
//                    System.out.println("-> " + userName + ": I am alive");
                }
            }catch (IOException e)
            {
                e.printStackTrace();
            } catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
        });
        imavThread.start();
//        System.out.println("-> imav thread: " + imavThread.getName());
    }

    private void sendQuitCommand(String command)
    {
        if(command.equalsIgnoreCase("quit"))
        {
            try
            {
                outputToServer.write("QUIT\n");
                outputToServer.flush();
//                outputToServer.close();
            } catch (IOException e)
            {
                e.printStackTrace();
            }
            console.close();
        }
        else
            {
                System.out.println("-> unknown command: " + command + "\n");
            }
    }
}

class Receiver extends Thread
{
    Socket socket;
    BufferedReader readFromServer;

    Receiver(Socket socket)
    {
        this.socket = socket;
        try
        {
            readFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void run()
    {
        getMsgFromServer();
    }

    private void getMsgFromServer()
    {
        String msgFromServer;
        try
        {
            while((msgFromServer = readFromServer.readLine()) != null)
            {
                System.out.println(msgFromServer);
                System.out.println();
            }
        } catch (IOException e)
        {
//            System.out.println(" -> Read stream has leftovers!");
            e.printStackTrace();
        }
    }
}

