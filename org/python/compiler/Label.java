// Copyright (c) Corporation for National Research Initiatives

package org.python.compiler;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

class Label {
    int position;
    int[] offsets, positions, sizes;
    int noffsets;
    Code code;
    int stack;

    public Label(Code code) {
        this.code = code;
        position = -1;
        noffsets = 0;
        offsets = new int[4];
        positions = new int[4];
        sizes = new int[4];
        stack = -1;
    }

    public void fix(byte[] data) throws IOException {
        ByteArrayOutputStream array = new ByteArrayOutputStream();
        DataOutputStream stream = new DataOutputStream(array);

        if (noffsets > 0 && position == -1)
            throw new InternalError("position never set for label");

        for (int i=0; i<noffsets; i++) {
            //System.out.println("o: "+offsets[i]+", "+position+", "+
            //                   positions[i]);
            int off = position-offsets[i];
            int p = positions[i];
            if (sizes[i] == 2) {
                stream.writeShort(off);
            } else {
                stream.writeInt(off);
            }

            System.arraycopy(array.toByteArray(), 0, data, p, sizes[i]);
            array.reset();
            //data[p] = (byte)(off >>> 8);
            //data[p+1] = (byte)(off & 0xff00);
        }
    }


    public void setStack(int stack) {
        if (this.stack == -1) {
            this.stack = stack;
        } else {
            if (this.stack != stack) {
                throw new InternalError("stack sizes don't agree: "+
                                        this.stack+", "+stack);
            }
        }
    }

    public int getPosition() {
        if (position == -1)
            throw new InternalError("position never set for label");
        return position;
    }

    public void setPosition() {
        position = code.size();
        //code.addLabel(this);
    }

    public void setBranch(int offset, int size) throws IOException {
        if (noffsets >= offsets.length) {
            int[] new_offsets = new int[offsets.length*2];
            System.arraycopy(offsets, 0, new_offsets, 0, noffsets);
            offsets = new_offsets;

            int[] new_positions = new int[positions.length*2];
            System.arraycopy(positions, 0, new_positions, 0, noffsets);
            positions = new_positions;

            int[] new_sizes = new int[sizes.length*2];
            System.arraycopy(sizes, 0, new_sizes, 0, noffsets);
            sizes = new_sizes;
        }
        positions[noffsets] = code.size();
        offsets[noffsets] = offset;
        sizes[noffsets] = size;
        noffsets = noffsets+1;
        if (size == 2) {
            code.code.writeShort(0);
        } else {
            code.code.writeInt(0);
        }
    }
}
