import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class ServerTest
{
    private static BufferedReader in;
    private static PrintWriter out;

    public static void main(String[] args) throws IOException
    {
        DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");  //for printing the time on the console
        Calendar cal = Calendar.getInstance();

        TimeTest timeTest = new TimeTest();
        timeTest.setTime();
        long timeThen = timeTest.getTime();

        System.out.println(dateFormat.format(timeThen));
    }


}

class TimeTest
{
    long time;

    TimeTest(){}

    void setTime()
    {
        Calendar calendar = Calendar.getInstance();
        long thenTime =
        time = calendar.getTimeInMillis();
    }

    long getTime()
    {
        return time;
    }
}
