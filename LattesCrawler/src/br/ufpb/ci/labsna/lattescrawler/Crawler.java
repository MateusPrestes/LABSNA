package br.ufpb.ci.labsna.lattescrawler;

import java.io.IOException;
import java.util.Enumeration;


/**
 * @author Alexandre N�brega Duarte - alexandre@ci.ufpb.br - http://alexandrend.com
 * 
 * Sintaxe para uso:  java LattesCrawler arquivo_de_entrada profundidade 
 * 
 * Onde arquivo_de_entrada contem um ou mais identificadores n�meros de curr�culos lattes
 * e profundidade � um n�mero inteiro indicando a dist�ncia a partir do curr�culo inicial que deve ser percorrida.
 * 
 * A sa�da ser� impressa na sa�da padr�o e representa uma descri��o em formato GML de um grafo.
 * 
 */
class Crawler implements Runnable {

	private LattesCrawler lc;
	
	private String lattesID;

	
	public Crawler(LattesCrawler lc, String lattesID) {
		this.lc = lc;
		this.lattesID = lattesID;
	}
	
	public void run(){
		
			//System.err.println( "START Thread" + this);
		
			
			//N�o queremos sobrecarregar o site do CNPQ. :-)
			try {
				Thread.sleep( (long) (Math.random() * 2000) );
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			        
			Lattes l = new Lattes(lattesID);
			try {
				l.extractData(lc); //enxtrai lates do crwaler
				lc.addLattes(l);
				
				Enumeration <String> con = l.getConnections().keys();
				while( con.hasMoreElements()) {
					String ol = con.nextElement();
					if( !lc.visited(ol)) {
						lc.addSeed(ol);				
					}
				}
			} catch (IOException e) {
				System.err.println( e + " http://lattes.cnpq.br/" + lattesID);
	
			} catch (LattesNotFoundException e) {
				// TODO Auto-generated catch block
				System.err.println(e);
			}
			
			//System.err.println( "END Thread" + this);
			

		
	}

}