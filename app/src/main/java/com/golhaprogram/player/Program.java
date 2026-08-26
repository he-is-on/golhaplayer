package com.golhaprogram.player;

public class Program {
    public final String category;
    public final String prefix;
    public final String number;
    public String singer = "";

    public Program(String category, String prefix, String number) {
        this.category = category;
        this.prefix = prefix;
        this.number = number;
    }

    public String audioUrl() {
        return "https://music.golhaprogram.com/" + prefix + "_" + number + ".mp3";
    }

    public String displayName() {
        if (singer == null || singer.trim().isEmpty()) {
            return "برنامه " + number;
        }
        return "برنامه " + number + " — " + singer;
    }
}
