package com.example.voiceassistant.speech;

import java.util.ArrayList;
import java.util.List;

public class TestCommandParser {
    public static void main(String[] args) {
        System.out.println("Testing CommandParser...");
        List<String> tests = new ArrayList<>();
        tests.add("Mở nhận diện vật thể");
        tests.add("mở camera");
        tests.add("bật nhận diện");
        tests.add("nhận diện vật thể");
        tests.add("quét vật thể");
        tests.add("quét xung quanh");
        tests.add("xem phía trước");
        tests.add("phía trước có gì");
        tests.add("nhìn giúp tôi");
        tests.add("camera");
        tests.add("mở chế độ nhìn");
        tests.add("tắt camera");
        tests.add("đóng camera");
        tests.add("thoát nhận diện");
        tests.add("về trang chủ");
        tests.add("trở về");
        tests.add("quay lại");
        tests.add("giúp tôi");
        tests.add("hướng dẫn");
        tests.add("nhắc lại");
        tests.add("đọc lại");
        tests.add("hủy");
        tests.add("open object detection");
        tests.add("start object detection");
        tests.add("detect objects");
        tests.add("recognize objects");
        tests.add("find objects");
        tests.add("scan objects");
        tests.add("turn on camera");
        tests.add("start camera");
        tests.add("look around");
        tests.add("identify object");
        tests.add("what is in front of me");
        tests.add("what do you see");
        tests.add("see surroundings");
        tests.add("start vision");
        tests.add("object mode");
        tests.add("detect surroundings");
        tests.add("recognize surroundings");
        tests.add("look ahead");
        tests.add("stop object detection");
        tests.add("close object detection");
        tests.add("exit camera");
        tests.add("stop camera");
        tests.add("turn camera off");
        tests.add("go home");
        tests.add("back home");
        tests.add("close vision");
        tests.add("cancel object detection");
        tests.add("stop recognition");
        tests.add("go home");
        tests.add("back");
        tests.add("return");
        tests.add("main screen");
        tests.add("home screen");
        tests.add("exit");
        tests.add("close current screen");
        tests.add("help");
        tests.add("what can I say");
        tests.add("voice commands");
        tests.add("instructions");
        tests.add("show commands");
        tests.add("how do I use this");
        tests.add("repeat");
        tests.add("say again");
        tests.add("repeat that");
        tests.add("speak again");
        tests.add("last message");
        tests.add("cancel");
        tests.add("never mind");
        tests.add("ignore");
        tests.add("stop listening");
        tests.add("object detect");
        tests.add("detect object");
        tests.add("object detector");
        tests.add("objects");
        tests.add("camera detection");
        tests.add("vision detection");
        tests.add("recognition mode");
        
        int passed = 0;
        int failed = 0;

        for (String test : tests) {
            CommandParser.CommandResult result = CommandParser.parse(test);
            System.out.println("Test: '" + test + "' -> Intent: " + result.getIntent().name());
            if (result.getIntent() == VoiceIntent.UNKNOWN) {
                failed++;
            } else {
                passed++;
            }
        }
        
        System.out.println("Passed: " + passed + " / Failed: " + failed);
    }
}
