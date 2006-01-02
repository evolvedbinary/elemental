
package org.exist.dom;

import java.io.UnsupportedEncodingException;

import org.exist.storage.Signatures;
import org.exist.util.ByteConversion;
import org.w3c.dom.Node;
import org.w3c.dom.ProcessingInstruction;

/**
 * Persistent implementation of a DOM processing-instruction node.
 * 
 * @author wolf
 */
public class ProcessingInstructionImpl extends StoredNode implements ProcessingInstruction {

    protected String target;
    protected String data;

    public ProcessingInstructionImpl() {
        super(Node.PROCESSING_INSTRUCTION_NODE);
    }

    public ProcessingInstructionImpl( long gid ) {
        super( Node.PROCESSING_INSTRUCTION_NODE, gid );
    }


    /**
     *  Constructor for the ProcessingInstructionImpl object
     *
     *@param  gid     Description of the Parameter
     *@param  target  Description of the Parameter
     *@param  data    Description of the Parameter
     */
    public ProcessingInstructionImpl( long gid, String target, String data ) {
        super( Node.PROCESSING_INSTRUCTION_NODE, gid );
        this.target = target;
        this.data = data;
    }
    
    public void clear() {
        super.clear();
        target = null;
        data = null;
    } 

    /**
     *  Gets the target attribute of the ProcessingInstructionImpl object
     *
     *@return    The target value
     */
    public String getTarget() {
        return target;
    }

    /**
     *  Sets the target attribute of the ProcessingInstructionImpl object
     *
     *@param  target  The new target value
     */
    public void setTarget( String target ) {
        this.target = target;
    }

	/* (non-Javadoc)
	 * @see org.w3c.dom.Node#getNodeName()
	 */
	public String getNodeName() {
		return target;
	}

    /**
     *  Gets the data attribute of the ProcessingInstructionImpl object
     *
     *@return    The data value
     */
    public String getData() {
        return data;
    }

    /**
     *  Sets the data attribute of the ProcessingInstructionImpl object
     *
     *@param  data  The new data value
     */
    public void setData( String data ) {
        this.data = data;
    }

    /**
     *  Description of the Method
     *
     *@return    Description of the Return Value
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append( "<?" );
        buf.append( target );
        buf.append( " " );
        buf.append( data );
        buf.append( " ?>" );
        return buf.toString();
    }


    /**
     *  Description of the Method
     *
     *@return    Description of the Return Value
     */
    public byte[] serialize() {
        byte[] td;
        byte[] dd;
        try {
            td = target.getBytes( "UTF-8" );
            dd = data.getBytes( "UTF-8" );
        } catch ( UnsupportedEncodingException uee ) {
            td = target.getBytes();
            dd = data.getBytes();
        }
        byte[] d = new byte[td.length + dd.length + 5];
        d[0] = (byte) ( Signatures.Proc << 0x5 );

        ByteConversion.intToByte( td.length, d, 1 );
        System.arraycopy( td, 0, d, 5, td.length );
        System.arraycopy( dd, 0, d, 5 + td.length, dd.length );
        return d;
    }

    public static StoredNode deserialize( byte[] data, int start, int len, boolean pooled ) {
        int l = ByteConversion.byteToInt( data, start + 1 );
        String target;
        String cdata;
        try {
            target = new String( data, start + 5, l, "UTF-8" );
            cdata = new String( data, start + 5 + l, len - 5 - l, "UTF-8" );
        } catch ( UnsupportedEncodingException uee ) {
            target = new String( data, start + 5, l );
            cdata = new String( data, start + 5 + l, len - 5 - l );
        }
        ProcessingInstructionImpl pi;
        if(pooled)
            pi = (ProcessingInstructionImpl)
				NodeObjectPool.getInstance().borrowNode(ProcessingInstructionImpl.class);
        else
            pi = new ProcessingInstructionImpl();
        pi.target = target;
        pi.data = cdata;
        return pi;
    }
    
    public boolean hasChildNodes() {
        return false;        
    }
    
    public Node getFirstChild() {   
        //bad implementations don't call hasChildNodes before
        return null;
    }       

}

