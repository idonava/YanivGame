package com.yaniv.online;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Stack;
import java.util.Vector;

/**
 *
 * @author Ido
 */
public abstract class Player {

    public Vector<Card> cards;
    public String name;
    public int index;
    public int sum;
    public boolean isPC;
    public int lastDropType;

    public Player(String name, int index) {
        this.name = name;
        this.index = index;
        cards = new Vector<>();
        sum = 0;
        lastDropType=0;
    }

    void calculateSum() {
        sum = 0;
        for (Card card : cards) {
            sum += card.v;
        }
    }

    public int[] dropCards(Stack<ArrayList<Card>> primaryPot) {
        return new int[0];
    }

    public int checkYanivOpportunity() {
        if (sum < 6) {
            return declareYaniv();
        }
        return 0;
    }

    public int declareYaniv() {
        return 0;
    }
    public int takeFrom(){ // Ask if to take from primaryjackpot or jack[ppt
        
        return 1;
    }
    public Vector<Card> getCards() {
        return cards;
    }

    public String getName() {
        return name;
    }

    public int getIndex() {
        return index;
    }

    public int getSum() {
        return sum;
    }

}
