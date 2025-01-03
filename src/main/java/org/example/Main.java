package org.example;

import java.io.IOException;
import java.util.List;


public class Main {
    public static void main(String[] args) throws IOException {
        InvertedIndex index = new InvertedIndex(2, List.of("14_8.txt", "9956_2.txt", "7_1.txt"));
        System.out.println(index.getDocuments("i"));
    }
}