package com.yaniv.online;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 * @author Ido
 */
public class Card implements Comparable<Card> {

    int n;
    char s;
    int v;

 //   String resourceName = "";

    public int getN() {
        return n;
    }

    public char getS() {
        return s;
    }

    public int getV() {
        return v;
    }
    public String getKey() {
        return ""+n+s;
    }
 /*   public String getResourceName() {
        return resourceName;
    }*/

    public Card(int num, char symbol) {
        this.n = num;
        this.s = symbol;
        if (num > 10) {
            this.v = 10;
        } else {
            this.v = num;
        }
      /*  resourceName += "c_";

        if (n == 14) {
            v = 0;
            resourceName = n + "_of_black";
        } else if (n == 15) {
            v = 0;
            resourceName = n + "_of_red";
        } else {
            resourceName = n + "_of_";
            if (s == '\u2764') { //hearts.
                resourceName += "hearts";

            } else if (s == '\u2660') {//spades.
                resourceName += "spades";

            } else if (s == '\u2666') {//diamonds
                resourceName += "diamonds";

            } else {//clubs
                resourceName += "clubs";
            }
        }*/
    }


    @Override
    public String toString() {
        return "" + n + s;
    }

    @Override
    public int compareTo(Card car) {
        if (this.n == car.n) {
            return 0;
        } else if (this.n == 14) {
            return -1;
        } else if (car.n == 14) {
            return 1;
        } else {
            return this.n > car.n ? 1 : -1;
        }
    }


}
