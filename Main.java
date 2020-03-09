package main.kyjko.zsirkreta;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferStrategy;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import org.json.*;

import javax.swing.*;

import static java.time.DayOfWeek.*;
import static java.time.temporal.TemporalAdjusters.next;

public class Main extends Canvas implements Runnable{

    public static final String apiKey = "7856d350-1fda-45f5-822d-e1a2f3f1acf0";
    public static String serverName = "https://klik035228001.e-kreta.hu/idp/api/v1/Token";
    public static String institutionCode = "klik035228001";
    public static String userName;
    public static String password;
    public static String miscStuff = "&grant_type=password&client_id=919e0c1c-76a2-4646-a2fb-7085bbbf3c56";
    public static String contentDescriptor = "Content-Type: application/x-www-form-urlencoded; charset=utf-8";
    public static String bearerCode;

    public static LocalDateTime now, then;
    public static String currentDate, nextDate;

    public static int WIDTH = 800, HEIGHT = 1000;

    //<JSON_OBJECTS>////////////////////////
    public static JSONArray TIMETABLE = null;
    public static JSONArray GRADES = null;
    //</JSON_OBJECTS>///////////////////////

    private Thread mainThread;
    private boolean running = false;

    boolean isSwitched = false;

    //grades scrolling stuff
    int scrollAmount = 0;

    //authenticate logic
    public static boolean isAuthed = false;

    private static ArrayList<Particle> parts;

    ////////////////////////////////////

    public static String getBearer() {
        String[] getBearerCode = {
                "curl",
                "-s",
                "--data",
                "institute_code=" + institutionCode + "&userName=" + userName + "&password=" + password + miscStuff,
                serverName,
                "-H",
                contentDescriptor

        };

        ProcessBuilder pb = new ProcessBuilder(getBearerCode);
        pb.redirectErrorStream(true);

        String curlResult = "";
        String line = "";
        try {
            Process process = pb.start();
            BufferedReader br = new BufferedReader(new InputStreamReader((process.getInputStream())));
            while((line = br.readLine()) != null) {
                //if(!line.equals("")) {
                    //if(line.substring(0, 1).equals("{")) {
                        curlResult += line;
                        //System.out.println(curlResult[3] + "\n" + curlResult[13]);
                    //}

                //}
            }
        } catch(Exception ex) { System.err.println("Something went wrong...\n"); ex.printStackTrace(); }

        isAuthed = true;
        System.out.println(curlResult);
        JSONObject obj = new JSONObject(curlResult);
        return obj.getString("access_token");
    }

    /////////////////////////////////////

    public JSONObject getGrades(String fromDate, String toDate) {
        String[] getGrades = {
                "curl",
                "-s",
                "-H",
                "Authorization: Bearer " + bearerCode,
                "https://klik035228001.e-kreta.hu/mapi/api/v1/Student?fromDate=" + fromDate + "&toDate=" + toDate
        };
        ProcessBuilder pb = new ProcessBuilder(getGrades);
        pb.redirectErrorStream(true);

        String curlResult = "";
        String line = "";
        try {
            Process process = pb.start();
            BufferedReader br = new BufferedReader(new InputStreamReader((process.getInputStream())));
            while((line = br.readLine()) != null) {
                if(!line.equals("")) {
                    curlResult += line;
                }
            }
        } catch(Exception ex) { System.err.println("Something went wrong...\n"); ex.printStackTrace(); }

        //convert raw string result into jsonarray
        JSONObject responseArray = new JSONObject(curlResult);

        return responseArray;

    }

    /////////////////////////////////////

    public JSONArray getTimetable(String fromDate, String toDate) {
        String[] getGrades = {
                "curl",
                "-s",
                "-H",
                "Authorization: Bearer " + bearerCode,
                "Content-Type: application/json; charset=utf-8",
                "https://klik035228001.e-kreta.hu/mapi/api/v1/Lesson?fromDate=" + fromDate + "&toDate=" + toDate

        };

        ProcessBuilder pb = new ProcessBuilder(getGrades);
        pb.redirectErrorStream(true);

        String curlResult = "";
        String line = "";
        try {
            Process process = pb.start();
            BufferedReader br = new BufferedReader(new InputStreamReader((process.getInputStream())));
            while((line = br.readLine()) != null) {
                if(!line.equals("")) {
                    curlResult += line;
                }
            }
        } catch(Exception ex) { System.err.println("Something went wrong...\n"); ex.printStackTrace(); }

        //convert raw string result into jsonarray
        JSONArray responseArray = new JSONArray(curlResult);

        return responseArray;
    }

    /////////////////////////////////////

    /////////////<DATE_SETTING>////////////

    private static void setDate(){
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        now = LocalDateTime.now();
        then = now.with(next(FRIDAY));
        currentDate = dtf.format(now);
        nextDate = dtf.format(then);
        //now = LocalDateTime.now().with(next(MONDAY));
        //currentDate = dtf.format(now);

    }

    ////////////</DATE_SETTING>////////////

    public synchronized void stop(){
        try{
            mainThread.interrupt();
            running = false;
        } catch(Exception ex) {ex.printStackTrace();}
    }

    public synchronized void start(){
        mainThread = new Thread(this);
        mainThread.start();
        running = true;
    }

    public Main() {
        JFrame f = new JFrame("Zsírkréta");
        f.setSize(WIDTH, HEIGHT);
        f.setVisible(true);
        f.setResizable(false);
        f.setLocationRelativeTo(null);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        f.add(this);
        this.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {
                int c = e.getKeyCode();
                if(c == KeyEvent.VK_ESCAPE) {
                    stop();
                    System.exit(-1);
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {

            }
        });
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                isSwitched = isSwitched ? false : true;
            }
        });

        this.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                int amount = e.getWheelRotation();
                if(amount > 0) {
                    scrollAmount-=20;
                } else if(amount < 0) {
                    if(scrollAmount <= 0) {
                        scrollAmount += 20;
                    }
                }
            }
        });

        userName = JOptionPane.showInputDialog("Felhasználónév").trim();
        password = JOptionPane.showInputDialog("Jelszó:").trim();
        start();
        //wrap every json-related thing into try-catch cause nobody knows mi a fsz szokott néha tortenni
        try {
            bearerCode = getBearer();
            //get timetable, for testing use 2020-02-01 as getGrades's first parameter as its WAAAAAAY faster that way
            TIMETABLE = getTimetable(currentDate, nextDate);
            GRADES = getGrades("2019-09-03", "").getJSONArray("Evaluations"); //get all grades regardless of date
        }catch(Exception ex) {}

    }

    String previousDay = "";

    public void render() {
        BufferStrategy bs = this.getBufferStrategy();
        if(bs == null){
            this.createBufferStrategy(3);
            return;
        }

        Graphics g = bs.getDrawGraphics();
        Graphics2D g2d = (Graphics2D) g;

        //render background
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        int x = -1;
        //render timetable
        if(isAuthed) {
            if (TIMETABLE != null) {
                if (!isSwitched) {
                    for (int i = 0; i < TIMETABLE.length(); i++) {
                        JSONObject o = TIMETABLE.getJSONObject(i);

                        //get current day for reference
                        String currentDay = o.getString("Date").split("T")[0];

                        if (!currentDay.equals(previousDay)) {
                            x++;
                            previousDay = currentDay;
                        }
                        //get count of lesson
                        int count = o.getInt("Count") - 1;

                        String subject = o.getString("Subject");
                        String classroom = o.getString("ClassRoom");
                        //String teacher = o.getString("Teacher");

                        //set colors dynamically based on lesson count
                        g.setColor(new Color((count * 25 % 255), 150, 54));

                        g.fillRect(x * WIDTH / 5, count * 100, WIDTH / 5, 85);
                        g.setColor(Color.WHITE);
                        g.setFont(new Font("Sans Serif", Font.BOLD, 18));
                        g.drawString(subject, x * WIDTH / 5, 25 + (count * 100));
                        g.setFont(new Font("Sans Serif", Font.ITALIC, 12));
                        //g.drawString(teacher, x*WIDTH/5, 50 + (count * 100));
                        g.drawString(classroom, x * WIDTH / 5, 75 + (count * 100));
                    }
                } else {
                    //switched from timetable to grades
                    //render GRADES

                    //handle mouse wheel scrolling
                    g2d.translate(0, scrollAmount);

                    if (GRADES != null) {
                        for (int i = 0; i < GRADES.length(); i++) {
                            JSONObject o = GRADES.getJSONObject(i);
                            String jegy = o.getString("Value");
                            String suly = o.getString("Weight");
                            String subject = "";
                            try {
                                subject = o.get("Subject") != null ? o.getString("Subject") : o.getJSONObject("Jelleg").getString("Leiras");
                            }catch(JSONException ex) {}

                            int jegy_int = o.getInt("NumberValue");
                            String teacher = "";
                            try {
                                teacher = o.getString("Teacher");
                            } catch(JSONException ex) {}

                            Color gradeColor = new Color(40, 40, 40), foregroundColor = new Color(255, 255, 255);  //default values of colors because its good to have those
                            //set color based on mark
                            if (jegy_int == 1) {
                                gradeColor = Color.RED;
                                foregroundColor = Color.WHITE;
                            } else if (jegy_int == 2) {
                                gradeColor = Color.ORANGE;
                                foregroundColor = Color.BLACK;
                            } else if (jegy_int == 3) {
                                gradeColor = Color.YELLOW;
                                foregroundColor = Color.BLACK;
                            } else if (jegy_int == 4) {
                                gradeColor = new Color(0, 180, 0);
                                foregroundColor = Color.WHITE;
                            } else if (jegy_int == 5) {
                                gradeColor = Color.BLUE;
                                foregroundColor = Color.WHITE;
                            }

                            g.setColor(gradeColor);
                            g.fillRect(WIDTH / 4, i * 170, WIDTH / 2, 140);
                            g.setColor(foregroundColor);
                            g.setFont(new Font("Sans Serif", Font.BOLD, 60));
                            g.drawString(String.valueOf(jegy_int), WIDTH / 4, i * 170 + 80);
                            g.setFont(new Font("Sans Serif", Font.PLAIN, 30));
                            g.drawString(subject, WIDTH / 3, i * 170 + 50);
                            g.setFont(new Font("Sans Serif", Font.ITALIC, 20));
                            g.drawString(suly, WIDTH / 3, i * 170 + 90);
                            g.setFont(new Font("Sans Serif", Font.PLAIN, 15));
                            g.drawString(teacher, WIDTH/4, i*170 + 135);
                        }

                    } else {
                        g.setColor(Color.WHITE);
                        g.setFont(new Font("Sans Serif", Font.PLAIN, 50));
                        g.drawString("Jegyek betöltése...", WIDTH / 4, HEIGHT / 2);

                        for(Particle p:parts) {
                            p.render(g);
                        }
                    }

                }
            } else {
                g.setColor(Color.WHITE);
                g.setFont(new Font("Sans Serif", Font.PLAIN, 50));
                g.drawString("Betöltés...", WIDTH / 3, HEIGHT / 2);

                for(Particle p:parts) {
                    p.render(g);
                }
            }

        } else {
            //not authenticated yet, render particles
            g.setColor(Color.WHITE);
            g.setFont(new Font("Sans Serif", Font.PLAIN, 40));
            g.drawString("Bejelentkezés...", WIDTH/4, HEIGHT/2);

            for(Particle p:parts) {
                p.render(g);
            }
        }

        g.dispose();
        bs.show();
    }

    public static void main(String[] args) {
        //authenticate user right away and sets current date
        setDate();
        //init particles now
        parts = new ArrayList<Particle>();
        for(int i = 0; i < 250; i++) {
            parts.add(new Particle((float)Math.random()*WIDTH, (float)Math.random()*HEIGHT/2 + HEIGHT));
        }
        new Main();
    }

    @Override
    public void run() {
        while(running){
            render();
        }
    }
}
