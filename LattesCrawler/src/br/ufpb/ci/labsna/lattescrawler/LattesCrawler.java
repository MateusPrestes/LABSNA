package br.ufpb.ci.labsna.lattescrawler;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Alexandre N�brega Duarte - alexandre@ci.ufpb.br - http://alexandrend.com
 */
public class LattesCrawler {
	
	private Map<String,Integer> visited; //visitados 
	
	private List<String> seeds; //ids de curriculos lattes
	
	private Map <String,Lattes> cvs; //recuperados
	
	public LattesCrawler() {
		
		visited = (Map<String,Integer>) Collections.synchronizedMap(new HashMap<String,Integer>());
		seeds = (List<String>) Collections.synchronizedList(new LinkedList<String>());
		cvs = (Map<String,Lattes>) Collections.synchronizedMap(new HashMap<String,Lattes>());
		
	}
	
	//Curriculos j� recuperados.
	public void addLattes(Lattes l) {
		cvs.put(l.getLattesID(),l);
	}

	//define que o curriculo foi visitado
	public void setVisited(String lattesID) {
		visited.put(lattesID, new Integer(0));
	}

	//verifica se o curriculo foi visitado
	public boolean visited(String id) {
		return visited.get(id)!=null;
	}
		
	
	/*
	 * Obtem os cvs
	 */
	public Map<String,Lattes> crawl(int maxNivel,int maxYear) throws IOException {
		
		ExecutorService pool = Executors.newFixedThreadPool(200);
		
		int nivel = 0; //nivel inicial
		
		while( !seeds.isEmpty() && nivel <= maxNivel) { //enquanto não acabar as fontes e não chegar ao nivel maximo
			
			String next = seeds.remove(0); //remove o primeiro
			
			if(next.equals("@")) { 
				
				System.err.println("AGUARDANDO FIM DO NIVEL " + nivel);
				pool.shutdown(); //evita que o pool recebe novas tarefas
				
				while(!pool.isTerminated() ) { //espera se todas as tarefas foram concluidas
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				System.err.println( "FIM DO NIVEL " + nivel);
				System.err.println("SEEDS " + seeds.size());
				
				nivel++;
				seeds.add("@");
				pool = Executors.newFixedThreadPool(200);
				
			} else {
				pool.execute(new Crawler(this, next,maxYear));
			}
		}
		
		pool.shutdown();
		
		while(!pool.isTerminated() ) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return cvs;
		
	}
	
	public void addSeed(String seed) {
		seeds.add(seed);
		setVisited(seed);
	}
	
	public static void main(String args[]) throws IOException{ 

			Set <String> seed = new HashSet<String>();
		
			LattesCrawler crawler = new LattesCrawler();
			
			//System.out.println( "FILE " + args[0]);
			
			BufferedReader r = new BufferedReader(new FileReader(args[0]));
			String line;
			
			while( (line = r.readLine())!= null) {
				crawler.addSeed(line.trim());
				
				seed.add(line.trim());
				
			}
			
			r.close();
			
		    crawler.addSeed("@"); //Marca de fim de n�vel
		    
		    
		    Map<String,Lattes> lattes = crawler.crawl(Integer.parseInt(args[1]),Integer.parseInt(args[2]));
					 
		    
			System.out.println( "graph");
			System.out.println( "[");
			System.out.println("\tdirected 0");
			
			
			//Imprimir a defini��o dos n�s
			Iterator <String>it = lattes.keySet().iterator();
			while( it.hasNext() ) {
				
				Lattes l = lattes.get(it.next());
				
				if(l.getName() != null ) {
				
					System.out.println( "\tnode");
					System.out.println( "\t[");
					System.out.println( "\t\t id " + l.getLattesID());
					System.out.println( "\t\t label " + "\"" + l.getName() + "\"");
				
					if(l.getNivel() != null )
						System.out.println( "\t\t bolsa " + "\"" + l.getNivel() + "\"");
					
					System.out.println( "\t\t periodicos " + l.getArtigosPeriod());
					System.out.println( "\t\t conferencias " + l.getArtigosConfer());
					System.out.println( "\t\t orientacoesMestrado " + l.getOrientaMestrado());
					System.out.println( "\t\t orientacoesDoutorado " + l.getOrientaDoutorado());
					System.out.println( "\t\t bancasMestrado " + l.getBancasMestrado());
					System.out.println( "\t\t bancasDoutorado " + l.getBancasDoutorado());
					
					
					if(seed.contains(l.getLattesID()))
						System.out.println( "\t\t seed 1");
					else	
						System.out.println( "\t\t seed 0");
					
						
					
				
					System.out.println( "\t]");
				}
				
			}
			
			//Imprimir a defini��o dos arcos
			it = lattes.keySet().iterator();
			
			while( it.hasNext()) {
				
				Lattes l = lattes.get(it.next());
				Dictionary <String,Integer> d = l.getConnections();
				
				Enumeration<String> conn = d.keys();
				
				while(conn.hasMoreElements()) {
					Lattes dest = lattes.get(conn.nextElement());
					
					if( dest != null && !dest.getLattesID().equals(l.getLattesID()) && dest.getName()!=null) {
						
						int p = d.get(dest.getLattesID()).intValue();
						
						System.out.println( "\tedge");
						System.out.println( "\t[");
						System.out.println( "\t\t source " + l.getLattesID());
						System.out.println( "\t\t target " + dest.getLattesID());
						System.out.println( "\t\t value " + p);
						System.out.println( "\t]");
					}
			
				}
			}	
			System.out.println("]");
	}

}
