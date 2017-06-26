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
        return "" + n + s;
    }

    public Card(int num, char symbol) {
        this.n = num;
        this.s = symbol;
        if (num > 10 && num < 14) {
            this.v = 10;
        } else if (num > 13) {
            this.v = 0;
        } else {
            this.v = num;
        }
    }

    @Override
    public String toString() {
        return "" + n + s;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Card other = (Card) obj;
        if ((n != other.n) || (s != other.s))
            return false;
        return true;
    }

    @Override
    public int compareTo(Card car) {
        if (this.n == car.n) {
            return 0;
        } else if (this.n == 14 || this.n == 15) {
            return -1;
        } else if (car.n == 14 || car.n == 15) {
            return 1;
        } else {
            return this.n > car.n ? 1 : -1;
        }
    }


}
