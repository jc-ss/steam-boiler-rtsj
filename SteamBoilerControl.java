import javax.realtime.PriorityScheduler;
import javax.realtime.PriorityParameters;
import javax.realtime.PeriodicParameters;
import javax.realtime.RelativeTime;
import javax.realtime.RealtimeThread;
import javax.realtime.Clock;
import javax.realtime.AbsoluteTime;
import javax.realtime.MemoryArea;

public class SteamBoilerControl {
    
    // Parâmetros da caldeira
    static final int C = 1000;  // Capacidade máxima (litros)
    static final int M1 = 150;  // Limite mínimo (litros)
    static final int M2 = 850;  // Limite máximo (litros)
    static final int N1 = 400;  // Normal mínimo (litros)
    static final int N2 = 600;  // Normal máximo (litros)
    static final int V = 70;    // Vapor saindo (litros/s)
    static final int P = 50;    // Capacidade de cada bomba (litros/s)
    
    // Estado do sistema
    static volatile int nivelAgua = 500;      // Nível atual de água
    static volatile int saidaVapor = V;       // Vapor saindo
    static volatile boolean funcionaBomba1 = true; // Se as bombas estão com defeito ou não
    static volatile boolean funcionaBomba2 = true;
    static volatile boolean funcionaSensorAgua = true;
    static volatile boolean funcionaSensorVapor = true;
    
    // Modos de operação
    enum Mode { INICIALIZACAO, NORMAL, DEGRADADO, SALVAMENTO, PARADA_EMERGENCIA }
    static volatile Mode atualModo = Mode.INICIALIZACAO;
    
    // Controle das bombas
    static volatile boolean ligadaBomba1 = false;// Se estão ligadas ou não
    static volatile boolean ligadaBomba2 = false;
    
    // Contadores para controle de transições
    static int ciclosEstabilizacao = 0; // evita que troque de modo muito rápido
    static int ciclosModoEmergencia = 0;
    // so sai do modo de emergencia depois de alguns ciclos estabilizados

    public static void main(String[] args) {
        System.out.println("JamaicaVM Version: " + System.getProperty("java.vm.version"));
        System.out.println("RTSJ Implementation: " + System.getProperty("javax.realtime.version"));
        
        // Thread de controle (executa a cada 5s)
        int controlPriority = PriorityScheduler.instance().getMinPriority() + 20;
        PriorityParameters controlPriorityParams = new PriorityParameters(controlPriority);
        RelativeTime controlPeriod = new RelativeTime(5000, 0); // 5 segundos
        PeriodicParameters controlPeriodicParams = new PeriodicParameters(null, controlPeriod, null, null, null, null);
        
        RealtimeThread controlThread = new RealtimeThread(controlPriorityParams, controlPeriodicParams) {
            public void run() {
                System.out.println("SISTEMA DE CONTROLE DA CALDEIRA INICIADO");
                
                for (int ciclo = 1; ciclo <= 30; ciclo++) {
                    waitForNextPeriod();
                    
                    AbsoluteTime t = Clock.getRealtimeClock().getTime();
                    System.out.println("\n--- CICLO " + ciclo + " - Tempo: " + t.getMilliseconds() + "ms ---");
                    
                    // Simular algumas falhas em momentos específicos
                    if (ciclo == 10) {
                        funcionaBomba1 = false; // força bomba 1 a parar
                        System.out.println("FALHA BOMBA: bomba 1 não está funcionando!");
                    }
                    if (ciclo == 15) {
                        funcionaSensorAgua = false; // força sensor de agua a parar
                        System.out.println("FALHA SENSOR: sensor de água com defeito!");
                    }
                    if (ciclo == 20) {
                        funcionaBomba1 = true; // bomba 1 volta a funcionar
                        System.out.println("REPARO: bomba 1 reparada!");
                    }
                    if (ciclo == 25) {
                        funcionaSensorAgua = true; // sensor de água volta ao normal
                        System.out.println("REPARO: sensor de água reparado!");
                    }
                    if (ciclo == 26) {
                        funcionaBomba1 = false; 
                        System.out.println("FALHA BOMBA: bomba 1 não está funcionando!");
                    }
                    logicaControle();
                    mostraEstadoFisico();
                }
                
                System.out.println("\nSIMULAÇÃO CONCLUÍDA");
            }
        };
        
        // Thread de simulação física (executa a cada 1s)
        int simPriority = PriorityScheduler.instance().getMinPriority() + 10;
        PriorityParameters simPriorityParams = new PriorityParameters(simPriority);
        RelativeTime simPeriod = new RelativeTime(1000, 0); // 1 segundo
        PeriodicParameters simPeriodicParams = new PeriodicParameters(null, simPeriod, null, null, null, null);
        
        RealtimeThread simulationThread = new RealtimeThread(simPriorityParams, simPeriodicParams) {
            public void run() {
                for (int i = 0; i < 150; i++) { //150 porque executa 5 vezes mais que o controle
                    waitForNextPeriod();
                    atualizaSistemaFisico();
                }
            }
        };
        
        // Inicia as threads
        try {
            controlThread.start();
            simulationThread.start();
            
            // Aguarda conclusão das threads
            controlThread.join();
            simulationThread.join();
            
        } catch (InterruptedException e) {
            System.out.println("Thread interrompida: " + e.getMessage());
        }
    }
    
    static void logicaControle() {
        boolean estaNivelEmergencia = (nivelAgua <= M1 || nivelAgua >= M2);
        // se o nivel estiver muito baixo ou muito alto, muda para emergencia
        
        switch (atualModo) {
            case INICIALIZACAO: // estabiliza o sistema antes de entrar na operacao normal
                System.out.println("MODO: Inicialização");
                
                if (nivelAgua < N1) {
                    // se o nivel estiver abaixo do normal, liga bombas com base nelas funcionarem
                    ligadaBomba1 = funcionaBomba1;
                    ligadaBomba2 = funcionaBomba2;
                    System.out.println("Inicialização: Nível baixo (" + nivelAgua + "L) - Ligando bombas disponíveis");
                } else if (nivelAgua > N2) {
                    ligadaBomba1 = false;
                    ligadaBomba2 = false;
                    // nivel muito alto de agua, desliga tudo
                    System.out.println("Inicialização: Nível alto (" + nivelAgua + "L) - Desligando bombas");
                } else {
                    ligadaBomba1 = funcionaBomba1 && (nivelAgua < (N1 + N2) / 2);
                    ligadaBomba2 = false;
                    // se não não ta baixo nem alto, tá normal. Só mantém uma bomba ligada
                    System.out.println("Inicialização: Nível normal (" + nivelAgua + "L) - Controle básico");
                }
                
                // Só muda de modo após alguns ciclos tentando corrigir
                ciclosEstabilizacao++;
                if (ciclosEstabilizacao >= 3) {
                    if (estaNivelEmergencia) { // Niveis criticos, muda para modo emergencia
                        atualModo = Mode.PARADA_EMERGENCIA;
                        ciclosModoEmergencia = 0;
                        System.out.println("Nível crítico detectado - Mudando para MODO EMERGÊNCIA");
                    } else if (checaIntegridadeSistema()) {
                        atualModo = Mode.NORMAL;
                        ciclosEstabilizacao = 0;
                        System.out.println("Sistema estável - Mudando para MODO NORMAL");
                    } else {
                        // Decide próximo modo baseado nos problemas
                        if (!funcionaSensorAgua) { // sensor de agua estragou
                            atualModo = Mode.SALVAMENTO;
                            ciclosEstabilizacao = 0;
                            System.out.println("Sensor de água com falha - Mudando para MODO SALVAMENTO");
                        } else if (!funcionaBomba1 || !funcionaBomba2) { // uma das bombas estragou
                            atualModo = Mode.DEGRADADO;
                            ciclosEstabilizacao = 0;
                            System.out.println("Bomba com falha - Mudando para MODO DEGRADADO");
                        }
                    }
                }
                break;
                
            case NORMAL:
                System.out.println("MODO: Normal");
                
                // Verificar se deve ir para emergência
                if (estaNivelEmergencia) {
                    atualModo = Mode.PARADA_EMERGENCIA;
                    ciclosModoEmergencia = 0;
                    System.out.println("EMERGÊNCIA: Nível crítico (" + nivelAgua + "L)!\nMudando para MODO EMERGÊNCIA");
                } else if (!funcionaSensorAgua) {
                    atualModo = Mode.SALVAMENTO;
                    System.out.println("Sensor de água com falha - Mudando para MODO SALVAMENTO");
                } else if (!funcionaBomba1 || !funcionaBomba2) {
                    atualModo = Mode.DEGRADADO;
                    System.out.println("Bomba com falha - Mudando para MODO DEGRADADO");
                } else {
                    controleBombas_TudoBem();
                }
                break;
                
            case DEGRADADO:
                System.out.println("MODO: Degradado");
                
                if (estaNivelEmergencia) {
                    atualModo = Mode.PARADA_EMERGENCIA;
                    ciclosModoEmergencia = 0;
                    System.out.println("EMERGÊNCIA: Nível crítico (" + nivelAgua + "L)!\nMudando para MODO EMERGÊNCIA");
                } else if (!funcionaSensorAgua) {
                    atualModo = Mode.SALVAMENTO;
                    System.out.println("Sensor de água com falha - Mudando para MODO SALVAMENTO");
                } else if (funcionaBomba1 && funcionaBomba2) {
                    atualModo = Mode.NORMAL;
                    System.out.println("Bombas reparadas - Mudando para MODO NORMAL");
                } else {
                    controleBombas_BombaComFalha();
                }
                break;
                
            case SALVAMENTO:
                System.out.println("MODO: Salvamento");
                
                if (estaNivelEmergencia) {
                    atualModo = Mode.PARADA_EMERGENCIA;
                    ciclosModoEmergencia = 0;
                    System.out.println("EMERGÊNCIA: Nível crítico (" + nivelAgua + "L)!");
                } else if (funcionaSensorAgua) {
                    if (funcionaBomba1 && funcionaBomba2) {
                        atualModo = Mode.NORMAL;
                        System.out.println("Sistema reparado - Mudando para MODO NORMAL");
                    } else {
                        atualModo = Mode.DEGRADADO;
                        System.out.println("Sensor reparado - Mudando para MODO DEGRADADO");
                    }
                } else {
                    controleBombas_SemSensor();
                }
                break;
                
            case PARADA_EMERGENCIA:
                System.out.println("MODO: PARADA DE EMERGÊNCIA!");
                
                if (nivelAgua <= M1) {
                    // Nível muito baixo, tenta recuperar ligando tudo que funciona
                    if (funcionaBomba1) ligadaBomba1 = true;
                    if (funcionaBomba2) ligadaBomba2 = true;
                    System.out.println("EMERGÊNCIA: Nível crítico baixo - FORÇANDO bombas para recuperação!");
                    
                    // Verificar se está melhorando
                    if (nivelAgua > M1 + 20) { // Mínimo necessário + margem
                        ciclosModoEmergencia++;
                        System.out.println("Recuperação em progresso... Ciclos: " + ciclosModoEmergencia);
                        
                        if (ciclosModoEmergencia >= 2) {
                            System.out.println("Nível seguro atingido - Retornando à inicialização");
                            atualModo = Mode.INICIALIZACAO;
                            ciclosModoEmergencia = 0;
                            ciclosEstabilizacao = 0;
                        }
                    } else {
                        ciclosModoEmergencia = 0;
                    }
                    
                } else if (nivelAgua >= M2) {
                    // Nível muito alto - desligar tudo
                    ligadaBomba1 = false;
                    ligadaBomba2 = false;
                    System.out.println("EMERGÊNCIA: Nível crítico alto - Desligando bombas!");
                    
                    // Verificar se está melhorando
                    if (nivelAgua < M2 - 20) { 
                        ciclosModoEmergencia++;
                        System.out.println("Recuperação em progresso... Ciclos: " + ciclosModoEmergencia);
                        
                        if (ciclosModoEmergencia >= 2) {
                            System.out.println("Nível seguro atingido - Retornando à inicialização");
                            atualModo = Mode.INICIALIZACAO;
                            ciclosModoEmergencia = 0;
                            ciclosEstabilizacao = 0;
                        }
                    } else {
                        ciclosModoEmergencia = 0;
                    }
                } else {
                    // Nível não está mais em emergência
                    System.out.println("Nível não mais crítico - Retornando à inicialização");
                    atualModo = Mode.INICIALIZACAO;
                    ciclosModoEmergencia = 0;
                    ciclosEstabilizacao = 0;
                }
                
                System.out.println("----EMERGÊNCIA | Aguardando estabilização do sistema----");
                break;
        }
    }
    
    static boolean checaIntegridadeSistema() {
        // Verifica se o sistema está em condições seguras
        boolean nivelAguaSeguro = (nivelAgua > M1 + 50 && nivelAgua < M2 - 50); // nível seguro é entre duas margens
        boolean nivelAguaNormal = (nivelAgua >= N1 && nivelAgua <= N2); // nível normal é entre duas margens
        boolean tudoFunciona = (funcionaSensorAgua && funcionaSensorVapor && 
                            (funcionaBomba1 || funcionaBomba2)); // Pelo menos uma bomba
        
        System.out.println("\nStatus do sistema atualmente:\nSeguro=" + nivelAguaSeguro + 
                          " Normal=" + nivelAguaNormal + 
                          " Dispositivos=" + tudoFunciona + "\n");
        
        return nivelAguaSeguro && tudoFunciona;
    }
    
    static void controleBombas_TudoBem() {
        // Controle baseado nos níveis normais N1 e N2
        if (nivelAgua < N1) {
            ligadaBomba1 = true;
            ligadaBomba2 = true;
            System.out.println("Nível abaixo do normal (" + nivelAgua + "L < " + N1 + "L) - Ligando ambas as bombas");
        } else if (nivelAgua > N2) {
            ligadaBomba1 = false;
            ligadaBomba2 = false;
            System.out.println("Nível acima do normal (" + nivelAgua + "L > " + N2 + "L) - Desligando bombas");
        } else if (nivelAgua < (N1 + N2) / 2) {
            ligadaBomba1 = true;
            ligadaBomba2 = false;
            System.out.println("Nível médio-baixo (" + nivelAgua + "L) - Ligando bomba 1");
        } else {
            ligadaBomba1 = false;
            ligadaBomba2 = false;
            System.out.println("Nível normal (" + nivelAgua + "L) - Bombas desligadas");
        }
    }
    
    static void controleBombas_BombaComFalha() {
        // Modo degradado: usar apenas bombas funcionais
        if (nivelAgua < N1) {
            if (funcionaBomba1) {
                ligadaBomba1 = true;
                System.out.println("Modo degradado - Bomba 1 ligada (nível " + nivelAgua + "L)");
            } else {
                ligadaBomba1 = false;
            }
            if (funcionaBomba2) {
                ligadaBomba2 = true;
                System.out.println("Modo degradado - Bomba 2 ligada (nível " + nivelAgua + "L)");
            } else {
                ligadaBomba2 = false;
            }
        } else if (nivelAgua > N2) {
            ligadaBomba1 = false;
            ligadaBomba2 = false;
            System.out.println("Modo degradado - Bombas desligadas (nível alto " + nivelAgua + "L)");
        } else {
            // Nível médio - usar uma bomba se disponível
            if (funcionaBomba1 && !funcionaBomba2) {
                ligadaBomba1 = true;
                ligadaBomba2 = false;
                System.out.println("Modo degradado - Apenas bomba 1 disponível");
            } else if (!funcionaBomba1 && funcionaBomba2) {
                ligadaBomba1 = false;
                ligadaBomba2 = true;
                System.out.println("Modo degradado - Apenas bomba 2 disponível");
            } else if (funcionaBomba1 && funcionaBomba2) {
                ligadaBomba1 = true;
                ligadaBomba2 = false;
                System.out.println("Modo degradado - Usando bomba 1 (economia)");
            }
        }
    }
    
    static void controleBombas_SemSensor() {
        // Controle baseado em estimativa
        if (saidaVapor < 50) { // Pouco vapor pode indicar pouca água
            if (funcionaBomba1) ligadaBomba1 = true;
            if (funcionaBomba2) ligadaBomba2 = true;
            System.out.println("Modo Salvamento: Estimando nível baixo pela baixa saída de vapor (" + saidaVapor + "L/s)");
        } else if (saidaVapor > 75) { // Muito vapor pode indicar muita água
            ligadaBomba1 = false;
            ligadaBomba2 = false;
            System.out.println("Modo Salvamento: Estimando nível alto pela alta saída de vapor (" + saidaVapor + "L/s)");
        } else {
            // Tenta manter nível com uma bomba funcional
            if (funcionaBomba1) {
                ligadaBomba1 = true;
                ligadaBomba2 = false;
            } else if (funcionaBomba2) {
                ligadaBomba1 = false;
                ligadaBomba2 = true;
            }
            System.out.println("Modo Salvamento: Controle conservativo baseado no vapor (" + saidaVapor + "L/s)");
        }
    }
    
    static void atualizaSistemaFisico() {
        // Calcula entrada de água
        int entradaAgua = 0;
        if (ligadaBomba1 && funcionaBomba1) entradaAgua += P;
        if (ligadaBomba2 && funcionaBomba2) entradaAgua += P;
        
        int saidaAgua = saidaVapor;
        int nivelAntigoAgua = nivelAgua;
        
        // Atualizar nível de água
        nivelAgua += entradaAgua - saidaAgua;
        
        // Limitar aos valores físicos possíveis
        if (nivelAgua < 0) nivelAgua = 0;
        if (nivelAgua > C) nivelAgua = C;
        
        // Simular variação no vapor baseada no nível de água
        int nivelAntigoVapor = saidaVapor;
        if (nivelAgua < 200) saidaVapor = 40; // Pouca água = pouco vapor
        else if (nivelAgua > 800) saidaVapor = 80; // Muita água = muito vapor
        else saidaVapor = V; // Vapor normal
        
        // Exibe mudanças significativas no sistema
        if (Math.abs(nivelAntigoAgua - nivelAgua) > 0 || nivelAntigoVapor != saidaVapor) {
            AbsoluteTime t = Clock.getRealtimeClock().getTime();
            System.out.println("[FÍSICA " + (t.getMilliseconds() % 100000) + "ms] " +
                              "Água: " + nivelAntigoAgua + "→" + nivelAgua + "L " +
                              "(+" + entradaAgua + " -" + saidaAgua + ") | " +
                              "Vapor: " + nivelAntigoVapor + "→" + saidaVapor + "L/s");
        }
    }
    
    static void mostraEstadoFisico() {
        System.out.println("Status: Água=" + nivelAgua + "L, Vapor=" + saidaVapor + "L/s");
        System.out.println("Bombas: Bomba1=" + (ligadaBomba1 ? "LIGADA" : "DESLIGADA") + 
                          " Bomba2=" + (ligadaBomba2 ? "LIGADA" : "DESLIGADA"));
        System.out.println("Funcionamento: |Bomba1=" + funcionaBomba1 + "|" + 
                          " |Bomba2=" + funcionaBomba2 + "|" +
                          " |SensorÁgua=" + funcionaSensorAgua + "|" + 
                          " |SensorVapor=" + funcionaSensorVapor + "|");
    }
}