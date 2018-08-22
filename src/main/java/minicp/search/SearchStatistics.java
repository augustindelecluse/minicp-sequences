/*
 * mini-cp is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License  v3
 * as published by the Free Software Foundation.
 *
 * mini-cp is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY.
 * See the GNU Lesser General Public License  for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with mini-cp. If not, see http://www.gnu.org/licenses/lgpl-3.0.en.html
 *
 * Copyright (c)  2017. by Laurent Michel, Pierre Schaus, Pascal Van Hentenryck
 */

package minicp.search;


public class SearchStatistics {

    private int nFailures = 0;
    private int nNodes = 0;
    private int nSolutions = 0;
    private boolean completed = false;

    public String toString() {
        return "\n\t#choice: " + nNodes +
                "\n\t#fail: " + nFailures +
                "\n\t#sols : " + nSolutions +
                "\n\tcompleted : " + completed + "\n";
    }

    public void incrFailures() { nFailures++; }
    public void incrNodes() { nNodes++; }
    public void incrSolutions() { nSolutions++; }
    public void setCompleted() { completed = true; }

    public int numberOfFailures() { return nFailures; }
    public int numberOfNodes() { return nNodes; }
    public int numberOfSolutions() { return nSolutions; }
    public boolean isCompleted() { return completed; }

}
