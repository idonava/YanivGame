/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.yaniv.online;

import android.os.Build;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Stack;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Ido
 */
public class PCPlayer {

    int Difficulty = 0;
    private int biggestSum;
    private int orderSum;
    private int equalSum;
    private int mySum;
    private static Vector<Card> myCards;
    String name;
    String mMyID;
    char hearts = '\u2764';
    char speades = '\u2660';
    char diamonds = '\u2666';
    char clubs = '\u2663';
    int lastDropType;
    int yanivMinScore;
    private static Stack<ArrayList<Card>> primaryDeck;
    private static Cards cardDeck;
    private boolean yanivDeclare = false;
    private boolean working;
    public boolean isYanivDeclare() {
        return yanivDeclare;
    }

    public PCPlayer(String name, String mMyID, int yanivMinScore) {
        this.name = name;
        this.mMyID = mMyID;
        this.yanivMinScore = yanivMinScore;
        calculateSum();
        // this.Difficulty = Difficulty;

    }

    private void calculateSum() {
        mySum = 0;
        for (Card card : myCards) {
            mySum += card.v;
        }
    }

    public boolean isWorking() {
        return working;
    }

    public void play(Stack<ArrayList<Card>> primaryDeck, Cards cardDeck, Vector<Card> myCards) {
        working=true;
        checkYanivOpportunity();
        if (!yanivDeclare) {
            this.primaryDeck = primaryDeck;
            this.cardDeck = cardDeck;
            this.myCards = myCards;
            cardDeckOnClick();
        }
        working=false;
    }

    public int[] dropCards() {
        //wait(500);
        biggestSum = -1;
        orderSum = -1;
        equalSum = -1;

        int biggestIndex = checkBiggestCard();

        int[] equalVector = checkEquals();
        int[] orderVector = checkFlush();

        //Checking if droping Sequence cards is the best option. 
        if (orderSum > biggestSum && orderSum > equalSum) {
            this.lastDropType = 3;
            return orderVector;
            //Checking if droping equals  cards is the best option. 
        } else if (equalSum > biggestSum && equalSum > orderSum) {
            this.lastDropType = 2;
            return equalVector;
            //Else drop the biggest card. 
        } else {
            this.lastDropType = 1;
            System.out.println("## " + name + " Drop " + myCards.get(biggestIndex).toString());
            //    wait(500);
            return new int[]{biggestIndex};
        }
    }

    public void checkYanivOpportunity() {
        if (mySum <= yanivMinScore) {
            this.yanivDeclare = true;
        } else {
            this.yanivDeclare = false;

        }
    }

    public void cardDeckOnClick() {
        //  Log.d(TAG, "cardDeckOnClick");
        final ArrayList<Card> myDrop = new ArrayList<Card>();
        int myCardsDrop[] = dropCards();
        for (int i = 0; i < myCardsDrop.length; i++) {
            int card = myCardsDrop[i];

            myDrop.add(myCards.get(card));
          //  Log.d(TAG, "Dropping: " + myCardsDrop.get(i) + " sym: " + myDrop.get(i));

        }
        for (int i = 0; i < myDrop.size(); i++) {
            myCards.remove(myDrop.get(i));
        }

        Card pop = cardDeck.jp.remove(0);
        myCards.add(pop);
        primaryDeck.add(myDrop);


    }

    public int checkBiggestCard() {
        this.biggestSum = -1;
        int index = 0;
        int biggest = 0;
        for (int i = 0; i < myCards.size(); i++) {
            if (myCards.get(i).v > biggest) {
                index = i;
                biggest = myCards.get(i).v;
            }
        }
        this.biggestSum = biggest;
        return index;
    }

    public int[] checkEquals() {
        equalSum = -1;
        Vector<Integer> Vec[] = (Vector<Integer>[]) new Vector[14];
        for (int i = 0; i < 14; i++) {
            Vec[i] = new Vector<>();
        }
        int sumArray[] = new int[14];
        boolean foundEqual = false;
        for (int i = 0; i < myCards.size(); i++) {
            Card c = myCards.get(i);
            Vec[c.n].add(i);
            sumArray[c.n] += c.v;
            if (Vec[c.n].size() > 1) {
                foundEqual = true;
            }
        }
        if (foundEqual) {
            int max = 0;
            int index = 0;
            for (int i = 1; i < sumArray.length; i++) {
                if (sumArray[i] >= max) {
                    max = sumArray[i];
                    index = i;
                }
            }
            equalSum = sumArray[index];
            return vecToInt(Vec[index]);

        } else {
            return null;
        }
    }

    public static int[] vecToInt(Vector<Integer> v) {
        int[] arr = new int[v.size()];
        for (int i = 0; i < v.size(); i++) {
            arr[i] = v.get(i);
        }
        return arr;
    }

    public static void wait(int time) {
        try {
            Thread.sleep(time);

        } catch (InterruptedException ex) {
            Logger.getLogger(PCPlayer.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    public int[] checkFlush() {
        orderSum = -1;
        int mat[][] = new int[5][14];
        for (int i = 0; i < 5; i++) {
            Arrays.fill(mat[i], -1);
            mat[i][0] = 0;
        }

        int sum = 0;
        int jokers = 0;
        Vector<Integer> ans = new Vector<>();
        //First index of arr is the count of the cards
        for (int i = 0; i < myCards.size(); i++) {
            Card c = myCards.get(i);
            if (c.s == hearts) {
                mat[0][c.n] = i;
                mat[0][0]++;
            } else if (c.s == speades) {
                mat[1][c.n] = i;
                mat[1][0]++;
            } else if (c.s == diamonds) {
                mat[2][c.n] = i;
                mat[2][0]++;
            } else if (c.s == clubs) { //sym = clubs
                mat[3][c.n] = i;
                mat[3][0]++;
            } else {
                mat[4][jokers++] = i;

            }
        }
        for (int i = 0; i < 4; i++) {
            if (mat[i][0] > 1) {
                for (int j = 1; j < 13; j++) {
                    int jokersTemp = jokers;
                    int tempSum = 0;
                    Vector<Integer> temp = new Vector<>();
                    if (mat[i][j] != -1) {
                        tempSum += myCards.get(mat[i][j]).v;
                        boolean found = false;
                        int next = j + 1;
                        int last = j;
                        temp.add(mat[i][j]);
                        tempSum += myCards.get(mat[i][j]).v;
                        while (!found && next < 14) {
                            if (mat[i][next] != -1) {
                                temp.add(mat[i][next]);
                                tempSum += myCards.get(mat[i][next]).v;
                                last = next;
                                next++;
                            } else if (jokersTemp > 0) {
                                if (next < 12) {
                                    if (jokersTemp == 2 && mat[i][next] == -1 && mat[i][next + 1] == -1 && mat[i][next + 2] != -1) {
                                        temp.add(mat[4][--jokersTemp]);
                                        temp.add(mat[4][--jokersTemp]);
                                        temp.add(mat[i][next + 2]);
                                        int v1 = next;
                                        if (v1 > 10) {
                                            v1 = 10;
                                        }

                                        int v2 = next + 1;
                                        if (v1 > 10) {
                                            v1 = 10;
                                        }
                                        int v3 = next + 2;
                                        if (v3 > 10) {
                                            v3 = 10;
                                        }
                                        tempSum += v1 + v2 + v3;
                                        last = next;
                                        next += 3;
                                    } else if (mat[i][next] == -1 && mat[i][next + 1] != -1) {
                                        temp.add(mat[4][--jokersTemp]);
                                        temp.add(mat[i][next + 1]);
                                        int v1 = next;
                                        if (v1 > 10) {
                                            v1 = 10;
                                        }
                                        tempSum += v1;
                                        last = next;
                                        next += 2;

                                    } else {
                                        found = true;
                                    }
                                } else {
                                    last = next;
                                    next++;

                                }
                            } else {

                                found = true;
                            }
                        }
                    }
                    if (tempSum > sum && temp.size() > 2) {
                        sum = tempSum;
                        ans = temp;
                    }
                }
            }
        }
        if (sum > 0) {
            orderSum = sum;
            return vecToInt(ans);
        } else {
            return null;
        }
    }
}
