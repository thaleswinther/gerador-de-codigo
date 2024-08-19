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

// Operadores Lógicos
OP_LOGICO: 'e' | 'ou';

// Símbolos de Controle
SIMBOLO_CONTROLE: ':' | '(' | ')' | '[' | ']' | '<-';

// Símbolos de Intervalo
INTERVALO: '..';

// Palavras reservadas e tokens
PALAVRAS_RESERVADAS: 'algoritmo' | 'fim_algoritmo' | 'var' |
                     'leia' | 'escreva' | 'se' | 'entao' | 'fim_se' | 'caso' | 'seja' | 'fim_caso' |
                     'para' | 'ate' | 'faca' | 'fim_para' | 'enquanto' | 'fim_enquanto' | 'registro' | 
                     'fim_registro' | 'retorne' | 'fim_procedimento' | 'fim_funcao';

DECLARE          : 'declare';
LITERAL          : 'literal';
INTEIRO          : 'inteiro';
REAL             : 'real';
LOGICO           : 'logico';
TRUE             : 'verdadeiro';
FALSE            : 'falso';
ELSE             : 'senao';
TIPO             : 'tipo';
CONSTANTE        : 'constante';
PROCEDIMENTO     : 'procedimento';
FUNCAO           : 'funcao';
PONTEIRO         : '^';
ENDERECO         : '&';
PONTO            : '.';
VIRGULA          : ',';
MENOR            : '<';
MENORIGUAL       : '<=';
MAIOR            : '>';
MAIORIGUAL       : '>=';
IGUAL            : '=';
DIFERENTE        : '<>';
NOT              : 'nao';

IDENT: LETRA (LETRA | DIGITO | '_')*;
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
declaracao_variavel: 'declare' variavel | 'constante' IDENT':' tipo_basico '=' tipo_constante | 'tipo' IDENT ':' estrutura;

tipo: tipo_estendido | estrutura;
tipo_basico: LITERAL | INTEIRO | REAL | LOGICO;
tipo_estendido: '^'? (tipo_basico | IDENT);
tipo_constante: CADEIA | NUMERO_INTEIRO | NUMERO_REAL | TRUE | FALSE;    
estrutura: 'registro' variavel* 'fim_registro';

// Regra de declaração de funções e procedimentos.
declaracao_global:
    'procedimento' IDENT '(' parametros? ')' declaracao_variavel* comando* 'fim_procedimento'  | 
    'funcao' IDENT '(' parametros? ')' ':' tipo_estendido declaracao_variavel* comando* corpo 'fim_funcao';

variavel: identificador (',' identificador)* ':' tipo;
identificador: IDENT ('.' IDENT)* dimensao;
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
cmdSe: 'se' expressao 'entao' cmdIf+=comando* ('senao' cmdElse+=comando*)? 'fim_se';
cmdCaso: 'caso' expressao_aritmetica 'seja' selecao ('senao' comando*)? 'fim_caso';
cmdPara: 'para' IDENT '<-' inicio=expressao_aritmetica 'ate' fim=expressao_aritmetica 'faca' comando* 'fim_para';
cmdEnquanto: 'enquanto' expressao 'faca' comando* 'fim_enquanto';
cmdFaca: 'faca' comando* 'ate' expressao;
cmdAtribuicao: '^'? identificador '<-' expressao;
cmdChamada: IDENT'(' expressao (',' expressao)* ')';
cmdRetorno: 'retorne' expressao;

selecao: item_selecao*;
item_selecao: constantes ':' comando*;
constantes: intervalo_numerico (',' intervalo_numerico)*;
intervalo_numerico: operador_inicio=operador_unario? inicio=NUMERO_INTEIRO ('..' operador_fim=operador_unario? fim=NUMERO_INTEIRO)?; 
operador_unario: '-';
expressao_aritmetica: termo (operador1 termo)*;
termo: fator (operador2 fator)*;
fator: parcela (operador3 parcela)*;
operador1: '+' | '-';
operador2: '*' | '/';
operador3: '%';
parcela: operador_unario? parcela_unaria | parcela_nao_unaria;
parcela_unaria: '^'? identificador | cmdChamada | NUMERO_INTEIRO | NUMERO_REAL | '(' exp_unica=expressao')';
parcela_nao_unaria: '&' identificador | CADEIA;
expressao_relacional: expressao_aritmetica (operador_relacional expressao_aritmetica)?;
operador_relacional: '=' | '<>' | '>=' | '<=' | '>' | '<';
expressao: termo_logico (operador_logico_ou termo_logico)*;
termo_logico: fator_logico (operador_logico_e fator_logico)*;
fator_logico: 'nao'? parcela_logica;
parcela_logica: ('verdadeiro' | 'falso') | expressao_relacional;
operador_logico_ou: 'ou';
operador_logico_e: 'e';