/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.camel.test.common.jibx;

import org.apache.camel.util.ObjectHelper;

public class PurchaseOrder {
    private String name;
    private double price;
    private int amount;

    public PurchaseOrder(String name, double price, int amount) {
        this.name = name;
        this.price = price;
        this.amount = amount;
    }

    public PurchaseOrder() {
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof PurchaseOrder) {
            PurchaseOrder that = (PurchaseOrder) o;
            return ObjectHelper.equal(this.name, that.name)
                    && ObjectHelper.equal(this.amount, that.amount)
                    && ObjectHelper.equal(this.price, that.price);
        }
        return false;
    }

    public int hashCode() {
        return (int) (name.hashCode() + (price * 100) + (amount * 100));
    }

    @Override
    public String toString() {
        return "PurchaseOrder[name: " + name + " amount: " + amount + " price: " + price + "]";
    }
}
