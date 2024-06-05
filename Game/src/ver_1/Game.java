package ver_1;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.swing.ImageIcon;

public class Game extends Thread {

    private Music gameMusic;
    private JAVA_Archive javaArchive;

    ArrayList<Note> noteList = new ArrayList<Note>();

    private String titleName;
    private String lastJudge = "";

    int combo = 0;
    public int maxCombo = 0;
    public int score = 0;
    public int missCount = 0;
    public int lateCount = 0;
    public int earlyCount = 0;
    public int goodCount = 0;
    public int greatCount = 0;
    public int perfectCount = 0;

    public Game(Music gameMusic, String musicName, JAVA_Archive javaArchive) {
        this.gameMusic = gameMusic;
        this.titleName = musicName;
        this.javaArchive = javaArchive;
        gameMusic.setOnCompletionListener(new Music.OnCompletionListener() {
            @Override
            public void onCompletion() {
                close();
            }
        });
        gameMusic.start();
    }

    public void screenDraw(Graphics2D g) {
        for (int i = 0; i < noteList.size(); i++) {
            Note note = noteList.get(i);

            if (note.getY() > 556) {
                score -= 10;
                combo = 0;
            }

            if (!note.isProceeded()) {
                noteList.remove(i);
                i--;
            } else {
                note.screenDraw(g);
            }
        }

        if (maxCombo < combo) {
            maxCombo = combo;
        }

        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.setColor(Color.CYAN);
        g.drawString("SCORE : ", 600, 250);
        g.drawString(String.valueOf(score), 740, 250);
        g.drawString("COMBO : ", 600, 280);
        g.drawString("MAX COMBO : ", 570, 310);
        g.drawString(String.valueOf(combo), 740, 280);
        g.drawString(String.valueOf(maxCombo), 740, 310);

        g.setFont(new Font("Arial", Font.BOLD, 30));
        g.setColor(Color.WHITE);
        g.drawString(String.valueOf(lastJudge), 180, 100);
    }

    public void pressD() {
        judge("D");
    }

    public void releaseD() {
    }

    public void pressF() {
        judge("F");
    }

    public void releaseF() {
    }

    public void pressJ() {
        judge("J");
    }

    public void releaseJ() {
    }

    public void pressK() {
        judge("K");
    }

    public void releaseK() {
    }

    @Override
    public void run() {
        dropNote();
    }

    public void close() {
    	try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        gameMusic.close();
        javaArchive.setState(3);
        this.interrupt();
    }

    public void dropNote() {
        Beat[] beats = null;

        ArrayList<Integer> time = new ArrayList<>();
        ArrayList<String> noteType = new ArrayList<>();

        InputStream in;
        BufferedReader br;
        String readfile = "";

        try {
            in = getClass().getResourceAsStream("../Asset/" + titleName + "_.txt");
            br = new BufferedReader(new InputStreamReader((in)));

            while ((readfile = br.readLine()) != null) {
                StringTokenizer note_stk = new StringTokenizer(readfile, " ");
                time.add(Integer.parseInt(note_stk.nextToken()));
                noteType.add(note_stk.nextToken());
            }

            int gap = 95 - Main.REACH_TIME;
            beats = new Beat[time.size()];

            for (int j = 0; j < time.size(); j++) {
                beats[j] = new Beat(time.get(j) + gap, noteType.get(j));
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }

        int i = 0;

        while (true) {
            boolean dropped = false;

            if (beats[i].getTime() <= gameMusic.getTime()) {
                Note note = new Note(beats[i].getNoteName());
                note.start();
                noteList.add(note);
                i++;

                dropped = true;
            }
            if (dropped) {
                try {
                    Thread.sleep(5);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void judge(String input) {
        for (int i = 0; i < noteList.size(); i++) {
            Note nowNote = noteList.get(i);

            if (input.equals(nowNote.getNoteType())) {
                judgeEvent(nowNote.judge());
                break;
            }
        }
    }

    public void judgeEvent(String judge) {
        if (judge.equals("Miss")) {
            score -= 10;
            combo = 0;
            lastJudge = "Miss";
            missCount++;
        } else if (judge.equals("Late")) {
            score += 5;
            combo += 1;
            lastJudge = "Late";
            lateCount++;
        } else if (judge.equals("Early")) {
            score += 10;
            combo += 1;
            lastJudge = "Early";
            earlyCount++;
        } else if (judge.equals("Good")) {
            score += 20;
            combo += 1;
            lastJudge = "Good";
            goodCount++;
        } else if (judge.equals("Great")) {
            score += 30;
            combo += 1;
            lastJudge = "Great";
            greatCount++;
        } else if (judge.equals("Perfect")) {
            score += 50;
            combo += 1;
            lastJudge = "Perfect";
            perfectCount++;
        }
    }

    public Music musicTime() {
        return gameMusic;
    }
}
