
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import javax.swing.JFrame;
import javax.swing.JPanel;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;



public class SerialPlotter extends JPanel {

	private static final long serialVersionUID = 1L;
	private static final float PREF_W = 860f; //para que queden 800 utiles, tras quitar 30 de dcha e izq
	private static final float PREF_H = 610f; //para qque queden 550 utiles, tras quitar 30 arriba y abajo
	private static final float BORDER_GAP = 0f; //el margen que se respeta entre el frame y el grafico
	private static final Color GRAPH_POINT_COLOR = Color.RED;
	private static final Stroke GRAPH_STROKE = new BasicStroke(1f); //3f
	private static final int POINT_WIDTH = 1;

	private static final float HPA_MAX = 955f; 
	private static final float HPA_MIN = 945f; 

	private static float SCALE; //
	
	
	public static void main ( String[] args )
	{
		SerialPlotter sp= new SerialPlotter();
		sp.init();
		SCALE=(PREF_H-2*BORDER_GAP)/(HPA_MAX-HPA_MIN);
		try {
			System.out.println("escala: "+SCALE);
			sp.connect("COM1");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}

	private void init() {


		JFrame frame = new JFrame("DrawGraph");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(this);
		frame.pack();
		frame.setLocationByPlatform(true);
		frame.setVisible(true);

	}

	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2= (Graphics2D) g;
		//      g2.clearRect(0, 0, frame.getWidth(),frame.getHeight());

		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// create x and y axes 
		g2.setStroke(GRAPH_STROKE);
		g2.drawLine((int)BORDER_GAP, getHeight() - (int)BORDER_GAP, (int)BORDER_GAP, (int)BORDER_GAP);
		g2.drawLine((int)BORDER_GAP, getHeight() - (int)BORDER_GAP, getWidth() - (int)BORDER_GAP, getHeight() - (int)BORDER_GAP);

		
		
	}

	public void plotLine(Graphics2D g2, Color c, double x, double y,double x1, double y1) {

		g2.setColor(c);
		g2.setStroke(GRAPH_STROKE);
		
		int xx=(int)Math.round(x);
		int yy= (int) Math.round(PREF_H+BORDER_GAP-PREF_H*((y-HPA_MIN)/(HPA_MAX-HPA_MIN)));

		int xx1=(int)Math.round(x1);
		int yy1= (int) Math.round(PREF_H+BORDER_GAP-PREF_H*((y1-HPA_MIN)/(HPA_MAX-HPA_MIN)));

   		g2.drawLine(xx, yy, xx1, yy1);

	}

	
	@Override
	public Dimension getPreferredSize() {
		return new Dimension(Math.round(PREF_W), Math.round(PREF_H));
	}

	void connect ( String portName  ) throws Exception {
		CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(portName);
		if ( portIdentifier.isCurrentlyOwned() )
		{
			System.out.println("Error: Port is currently in use");
		}
		else
		{
			CommPort commPort = portIdentifier.open(this.getClass().getName(),2000);

			if ( commPort instanceof SerialPort )
			{
				SerialPort serialPort = (SerialPort) commPort;
				serialPort.setSerialPortParams(9600,SerialPort.DATABITS_7,SerialPort.STOPBITS_1,SerialPort.PARITY_EVEN);

				InputStream in = serialPort.getInputStream();
				OutputStream out = serialPort.getOutputStream();

				(new Thread(new SerialReader(this, in))).start();
				(new Thread(new SerialWriter(out))).start();

			}
			else
			{
				System.out.println("Error: Only serial ports are handled by this example.");
			}
		}     
	}




	/**
	 * Serial Reader
	 * @author Tschuss
	 *
	 */
	private static class SerialReader implements Runnable {

		InputStream in;	 
		JPanel jp;

		float x=BORDER_GAP+POINT_WIDTH;

		int x_ant, y_ant=0;
		
		public SerialReader (JPanel jp, InputStream in )
		{
			this.in = in;
			this.jp=jp;	
		}

		public void run ()
		{
			byte[] buffer = new byte[1024];
			int len = -1;
			String line ="";


			try
			{
				while ( ( len = this.in.read(buffer)) > -1 )
				{
					if (len!=0) {
						String readed = new String(buffer,0,len);
						if (readed.indexOf("\n")==-1) {
							//no hay un final de linea
							line+=readed;
						} else {
							String [] parts= readed.split("\r\n");
							line+=parts[0];
							//System.out.println((new Date())+ " >>"+line+"<<");
							plotHPA (line);
							if (parts.length>1) {
								line=parts[1];
							}else {
								line="";
							}
						}
					}
				}
			}
			catch ( IOException e )
			{
				e.printStackTrace();
			}            
		}


		public void plotHPA (String a) {

			x++;
			String[] parts = a.trim().split("( )+");
			System.out.println(Arrays.asList(parts).toString());
			double y=0.0;
			try {
				y = Double.parseDouble(parts[2]) ; //3er valor del sensor, la media
			} catch (Exception e) {

				return;
			}
			y=y+BORDER_GAP; //para que se pinte respecto al eje X
			
			

			System.out.println("("+x+","+y+")");

			//plotDot(GRAPH_POINT_COLOR,x++,y);
			plotLine2Dot(GRAPH_POINT_COLOR,x,y);
		}
		
		public void plotDot(Color c, double x, double y) {
			Graphics2D g2= (Graphics2D) jp.getGraphics();
			g2.setColor(c);
			g2.setStroke(GRAPH_STROKE);
		//	y=y*SCALE;
			int xx=(int)Math.round(x);
			int yy= (int) Math.round(PREF_H+BORDER_GAP-PREF_H*((y-HPA_MIN)/(HPA_MAX-HPA_MIN)));
			//int yy=(int) Math.round(HPA_MAX+BORDER_GAP-y);
			System.out.println("plot["+xx+","+yy+"]");
    		
			//g2.fillOval(xx, yy, 2, 2);      
    		
		}

		public void plotLine2Dot(Color c, double x, double y) {
			Graphics2D g2= (Graphics2D) jp.getGraphics();
			g2.setColor(c);
			g2.setStroke(GRAPH_STROKE);

			int xx=(int)Math.round(x);
			int yy= (int) Math.round(PREF_H+BORDER_GAP-PREF_H*((y-HPA_MIN)/(HPA_MAX-HPA_MIN)));

			System.out.println("line["+x_ant+","+y_ant+","+xx+","+yy+"]");
    		if (x_ant!=0){
        		g2.drawLine(x_ant, y_ant, xx, yy);
    		}
    		x_ant=xx;
    		y_ant=yy;
		}	
		
		

	}


	/**
	 * SerialWriter
	 * @author Tschuss
	 *
	 */
	private static class SerialWriter implements Runnable 
	{
		OutputStream out;

		public SerialWriter ( OutputStream out )
		{
			this.out = out;
		}

		public void run ()
		{
			try
			{                
				int c = 0;
				while ( ( c = System.in.read()) > -1 )
				{
					System.out.println("<<"+c);
					this.out.write(c);
				}                
			}
			catch ( IOException e )
			{
				e.printStackTrace();
			}            
		}
	}



}
