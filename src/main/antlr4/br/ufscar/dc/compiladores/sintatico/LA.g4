grammar LA;

// Fragmentos utilizados para ajudar nas regras léxicas
fragment LETRA: [a-zA-Z];
fragment DIGITO: [0-9];
fragment SIMBOLO_ESPECIAL: ' ' | '(' | ')';

fragment TEXTO: (LETRA | DIGITO | SIMBOLO_ESPECIAL)*;

// Regras para detectar strings e comentários
CADEIA: '"' ~[\n"']* '"';
COMENTARIO: '{' ~[\n}]* '}' -> skip;

// Espaços em branco e caracteres de controle
PULAR_LINHA: '\r'? '\n' -> skip;
ESPACO: [ \t]+ -> skip;

// Operadores Aritméticos
OP_ARITMETICO: '+' | '-' | '*' | '/' | '%';

// Operadores Relacionais
OP_RELACIONAL: '<=' | '>=' | '=' | '<' | '<>' | '>';

// Operadores Lógicos
OP_LOGICO: 'e' | 'ou' | 'nao';

// Símbolos de Controle
SIMBOLO_CONTROLE: ':' | ',' | '(' | ')' | '[' | ']' | '<-' | '^' | '&';

// Símbolos de Intervalo
INTERVALO: '..';

// Palavras reservadas e tokens
PALAVRAS_RESERVADAS: 'algoritmo' | 'fim_algoritmo' | 'declare' | 'constante' | 'tipo' | 'var' |
                     'leia' | 'escreva' | 'inteiro' | 'real' | 'logico' | 'literal' |
                     'se' | 'entao' | 'senao' | 'fim_se' | 'caso' | 'seja' | 'fim_caso' |
                     'para' | 'ate' | 'faca' | 'fim_para' | 'enquanto' | 'fim_enquanto' |
                     'registro' | 'fim_registro' | 'procedimento' | 'funcao' | 'retorne' |
                     'fim_procedimento' | 'fim_funcao' | 'falso' | 'verdadeiro';

IDENTIFICADOR: LETRA (LETRA | DIGITO | '_')*;
NUMERO_INTEIRO: DIGITO+;
NUMERO_REAL: DIGITO+ ('.' DIGITO+)?;

// Comentários e cadeias não fechados
ERRO_COMENTARIO: '{' ~[}]* EOF;
ERRO_CADEIA: '"' ~[\n"']* '\n';

// Símbolos não reconhecidos
ERRO_SIMBOLO_INVALIDO: '~' | '$' | '}' | '|' | '!' | '@';

// Estrutura principal da gramática
programa: declaracoes 'algoritmo' corpo 'fim_algoritmo' EOF;
declaracoes: (declaracao_variavel | declaracao_global)*;

// Declarações e Definições de Tipos
declaracao_variavel: 'declare' variavel | 
                     'constante' IDENTIFICADOR ':' tipo_basico '=' tipo_constante | 
                     'tipo' IDENTIFICADOR ':' tipo;

tipo: tipo_basico | tipo_estendido | estrutura;
tipo_basico: 'literal' | 'inteiro' | 'real' | 'logico';
tipo_estendido: '^'? (tipo_basico | IDENTIFICADOR);
tipo_constante: CADEIA | NUMERO_INTEIRO | NUMERO_REAL | 'verdadeiro' | 'falso';
estrutura: 'registro' variavel* 'fim_registro';

declaracao_global:
    'procedimento' IDENTIFICADOR '(' parametros? ')' corpo 'fim_procedimento' | 
    'funcao' IDENTIFICADOR '(' parametros? ')' ':' tipo_estendido corpo 'fim_funcao';

variavel: identificador (',' identificador)* ':' tipo;
identificador: IDENTIFICADOR ('.' IDENTIFICADOR)* dimensao;
dimensao: ('[' expressao_aritmetica ']')*;

parametro: 'var'? identificador (',' identificador)* ':' tipo_estendido;
parametros: parametro (',' parametro)*;
corpo: declaracao_variavel* comando*;

// Agrupamento de comandos para melhor organização
comando: cmdLeitura | cmdEscrita | 
         cmdControleFluxo | 
         cmdAtribuicao | 
         cmdChamada | 
         cmdRetorno;

cmdControleFluxo: cmdSe | cmdCaso | cmdPara | cmdEnquanto | cmdFaca;

cmdLeitura: 'leia' '(' '^'? identificador (',' '^'? identificador)* ')';
cmdEscrita: 'escreva' '(' expressao (',' expressao)* ')';
cmdSe: 'se' expressao 'entao' comando* ('senao' comando*)? 'fim_se';
cmdCaso: 'caso' expressao_aritmetica 'seja' selecao ('senao' comando*)? 'fim_caso';
cmdPara: 'para' IDENTIFICADOR '<-' expressao_aritmetica 'ate' expressao_aritmetica 'faca' comando* 'fim_para';
cmdEnquanto: 'enquanto' expressao 'faca' comando* 'fim_enquanto';
cmdFaca: 'faca' comando* 'ate' expressao;
cmdAtribuicao: '^'? identificador '<-' expressao;
cmdChamada: IDENTIFICADOR '(' expressao (',' expressao)* ')';
cmdRetorno: 'retorne' expressao;

selecao: item_selecao*;
item_selecao: constantes ':' comando*;
constantes: intervalo_numerico (',' intervalo_numerico)*;
intervalo_numerico: operador_unario? NUMERO_INTEIRO ( '..' operador_unario? NUMERO_INTEIRO)?;
operador_unario: '-';
expressao_aritmetica: termo (operador1 termo)*;
termo: fator (operador2 fator)*;
fator: parcela (operador3 parcela)*;
operador1: '+' | '-';
operador2: '*' | '/';
operador3: '%';
parcela: operador_unario? parcela_unaria | parcela_nao_unaria;
parcela_unaria: '^'? identificador | IDENTIFICADOR '(' expressao (',' expressao)* ')' | NUMERO_INTEIRO | NUMERO_REAL | '(' expressao ')';
parcela_nao_unaria: '&' identificador | CADEIA;
expressao_relacional: expressao_aritmetica (operador_relacional expressao_aritmetica)?;
operador_relacional: '=' | '<>' | '>=' | '<=' | '>' | '<';
expressao: termo_logico (operador_logico_ou termo_logico)*;
termo_logico: fator_logico (operador_logico_e fator_logico)*;
fator_logico: 'nao'? parcela_logica;
operador_logico_ou: 'ou';
operador_logico_e: 'e';
parcela_logica: 'verdadeiro' | 'falso' | expressao_relacional;
