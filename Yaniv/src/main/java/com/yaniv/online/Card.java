package com.yaniv.online;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Ido
 */
public class Card implements Comparable<Card> {

    int n;
    char s;
    int v;

    public Card(int num, char symbol) {
        this.n = num;
        this.s = symbol;
        if (num > 10) {
            this.v = 10;
        } else {
            this.v = num;
        }
    }

    @Override
    public String toString() {
        return "" + n + s;
    }

    @Override
    public int compareTo(Card car) {
        if (this.n == car.n) {
            return 0;
        } else {
            return this.n > car.n ? 1 : -1;
        }
    }

}
