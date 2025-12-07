# Sistema de Controle de Caldeira a Vapor com RTSJ

Sistema de controle em tempo real para gerenciamento de uma caldeira a vapor, desenvolvido com Java Real-Time Specification (RTSJ) e JamaicaVM.

## 📋 Sobre o Projeto

Este projeto implementa um sistema de controle para uma caldeira a vapor que monitora e mantém o nível de água dentro de limites seguros. O sistema opera em diferentes modos de operação e é capaz de lidar com falhas em componentes físicos.

### Características Principais

- **Controle em Tempo Real**: Utiliza RTSJ para garantir execução determinística
- **Múltiplos Modos de Operação**: Inicialização, Normal, Degradado, Salvamento e Parada de Emergência
- **Tolerância a Falhas**: Detecta e responde a falhas em bombas e sensores
- **Simulação Física**: Simula o comportamento físico da caldeira em tempo real

## 🎯 Objetivos Acadêmicos

Projeto desenvolvido para a disciplina de Sistemas de Tempo Real da Universidade Federal de Pelotas (UFPel), com os seguintes objetivos:

- Modelagem e especificação de sistemas de tempo real
- Aplicação prática da Real-Time Specification for Java (RTSJ)
- Implementação de sistemas tolerantes a falhas
- Uso de threads periódicas com prioridades diferentes

## 🏗️ Arquitetura do Sistema
git push --force
### Componentes Físicos

- **Caldeira**: Capacidade de 1000 litros
- **2 Bombas**: Capacidade de 50 litros/segundo cada
- **Sensor de Nível de Água**: Monitora a quantidade de água
- **Sensor de Vapor**: Mede a saída de vapor (70 litros/segundo nominal)

### Limites Operacionais

| Parâmetro | Valor | Descrição |
|-----------|-------|-----------|
| C | 1000 L | Capacidade máxima |
| M1 | 150 L | Limite mínimo crítico |
| M2 | 850 L | Limite máximo crítico |
| N1 | 400 L | Limite mínimo normal |
| N2 | 600 L | Limite máximo normal |
| V | 70 L/s | Saída nominal de vapor |
| P | 50 L/s | Capacidade de cada bomba |

## 🔄 Modos de Operação

### 1. Inicialização
- Verifica o estado inicial do sistema
- Estabiliza o nível de água
- Transiciona para o modo apropriado após 3 ciclos

### 2. Normal
- Mantém o nível entre N1 (400L) e N2 (600L)
- Controle otimizado das bombas
- Monitoramento contínuo de falhas

### 3. Degradado
- Opera com uma bomba com falha
- Usa apenas equipamentos funcionais
- Mantém segurança com capacidade reduzida

### 4. Salvamento
- Opera sem sensor de nível de água
- Usa sensor de vapor para estimativas
- Controle conservativo baseado em heurísticas

### 5. Parada de Emergência
- Ativada quando níveis ultrapassam M1 ou M2
- Tenta recuperação automática
- Força ações corretivas (liga/desliga bombas)

## 🚀 Requisitos

### Software Necessário

- **Java Development Kit (JDK)**: Versão 8 ou superior
- **JamaicaVM**: Implementação RTSJ da AICAS
  - Download: https://www.aicas.com/jamaica/
  - Versão recomendada: 8.10 ou superior

### Documentação de Referência

- [Manual JamaicaVM](https://www.aicas.com/download/manuals/aicas-JamaicaVM-8.10-Manual.pdf)
- [API Jamaica](https://www.aicas.com/jamaica/8.10/doc/jamaica_api/index.html)
- [RTSJ Standards](https://www.aicas.com/standards/rtsj/)

## 💻 Instalação e Execução

### 1. Instalar JamaicaVM

```bash
# Extrair o pacote JamaicaVM
tar -xzf jamaica-8.10-linux-x64.tar.gz
cd jamaica-8.10

# Configurar variáveis de ambiente
export JAMAICA_HOME=/caminho/para/jamaica
export PATH=$JAMAICA_HOME/bin:$PATH
```

### 2. Compilar o Projeto

```bash
# Compilar com o compilador Jamaica
jamaicac SteamBoilerControl.java
```

### 3. Executar o Sistema

```bash
# Executar com a JamaicaVM
jamaica SteamBoilerControl
```

### Saída Esperada

O sistema executará 30 ciclos de controle (5 segundos cada), simulando:
- Operação normal inicial
- Falha da Bomba 1 (ciclo 10)
- Falha do Sensor de Água (ciclo 15)
- Reparo da Bomba 1 (ciclo 20)
- Reparo do Sensor de Água (ciclo 25)
- Nova falha da Bomba 1 (ciclo 26)

## 📊 Estrutura do Código

### Threads em Tempo Real

**Thread de Controle** (Prioridade: Min+20, Período: 5s)
- Executa a lógica de controle principal
- Decide modo de operação
- Controla estado das bombas

**Thread de Simulação Física** (Prioridade: Min+10, Período: 1s)
- Atualiza nível de água
- Simula saída de vapor
- Aplica efeitos das bombas

### Variáveis de Estado

```java
static volatile int nivelAgua = 500;              // Nível atual (litros)
static volatile int saidaVapor = V;               // Vapor saindo (L/s)
static volatile boolean funcionaBomba1 = true;    // Status bomba 1
static volatile boolean funcionaBomba2 = true;    // Status bomba 2
static volatile boolean ligadaBomba1 = false;     // Bomba 1 ligada?
static volatile boolean ligadaBomba2 = false;     // Bomba 2 ligada?
static volatile Mode atualModo;                   // Modo de operação
```

## 🔍 Cenários de Teste

O sistema simula automaticamente os seguintes cenários:

1. **Inicialização e Estabilização** (Ciclos 1-9)
2. **Falha de Bomba** (Ciclos 10-19)
3. **Falha de Sensor** (Ciclos 15-24)
4. **Recuperação Parcial** (Ciclos 20-25)
5. **Falha Recorrente** (Ciclos 26-30)

## 📈 Exemplo de Execução

```
--- CICLO 10 - Tempo: 50000ms ---
FALHA BOMBA: bomba 1 não está funcionando!
MODO: Normal
Bomba com falha - Mudando para MODO DEGRADADO
Status: Água=520L, Vapor=70L/s
Bombas: Bomba1=DESLIGADA Bomba2=DESLIGADA

--- CICLO 15 - Tempo: 75000ms ---
FALHA SENSOR: sensor de água com defeito!
MODO: Degradado
Sensor de água com falha - Mudando para MODO SALVAMENTO
Status: Água=450L, Vapor=70L/s
```

## 🛡️ Tratamento de Falhas

O sistema implementa estratégias robustas para diferentes tipos de falhas:

- **Falha de Bomba**: Modo Degradado - usa bombas funcionais
- **Falha de Sensor de Água**: Modo Salvamento - estima nível pelo vapor
- **Nível Crítico**: Parada de Emergência - ações corretivas forçadas
- **Múltiplas Falhas**: Degrada graciosamente para modo mais seguro

## 📚 Referências

- [ABR 96] Jean-Raymond Abrial. "The Steam Boiler Control Specification Problem"
- RTSJ Expert Group. "Real-Time Specification for Java"
- AICAS GmbH. "Jamaica Virtual Machine Documentation"

## 👥 Autores

Desenvolvido como trabalho acadêmico para a disciplina de Sistemas de Tempo Real - UFPel

**Disciplina**: Sistemas de Tempo Real  
**Instituição**: Universidade Federal de Pelotas  
**Curso**: Engenharia de Computação / Ciência da Computação  
**Semestre**: 2025/1

## 📝 Licença

Este projeto é de caráter acadêmico e está disponível para fins educacionais.

---

**Nota**: Este sistema é uma implementação educacional e não deve ser usado para controle de equipamentos reais sem validação e certificação apropriadas.
