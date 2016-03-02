/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package net.proest.lp2go3.UAVTalk;

public class UAVTalkDeviceHelper {

    public static byte[] createSettingsObjectByte(UAVTalkObjectTree oTree, String objectName, int instance, String fieldName, int element, byte[] newFieldData) {

        UAVTalkObject obj = oTree.getObjectNoCreate(objectName);
        if (obj == null) {
            return null;
        }
        UAVTalkObjectInstance ins = obj.getInstance(instance);
        if (ins == null) {
            return null;
        }

        UAVTalkXMLObject xmlObj = oTree.getXmlObjects().get(objectName);
        if (xmlObj == null) {
            return null;
        }
        UAVTalkXMLObject.UAVTalkXMLObjectField xmlField = xmlObj.getFields().get(fieldName);
        if (xmlField == null) {
            return null;
        }

        byte[] data = ins.getData();
        int fpos = xmlField.pos;
        int elen = xmlField.typelength;

        int savepos = fpos + elen * element;

        //if(newFieldData.length != elen) { return false; }  //ACTIVATE ME!

        System.arraycopy(newFieldData, 0, data, savepos, newFieldData.length);

        ins.setData(data);
        obj.setInstance(ins);
        oTree.updateObject(obj);

        byte[] send = obj.toMsg((byte) 0x20, ins.getId());
        return send;
    }
}