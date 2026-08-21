package com.techchallenge.lambda.authorizer.domain.model.valueobjects;

import com.techchallenge.lambda.authorizer.domain.exceptions.ValidacaoValorException;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.InputMismatchException;

@Getter
@EqualsAndHashCode
public final class CNPJ {
    private final String valor;

    private CNPJ(String valor) {
        this.valor = validar(valor);
    }

    public static CNPJ of(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new ValidacaoValorException("CNPJ não pode ser nulo ou vazio");
        }
        return new CNPJ(valor);
    }

    private String validar(String cnpj) {
        String cnpjLimpo = cnpj.replaceAll("\\D", "");

        if (!isValidCNPJ(cnpjLimpo)) {
            throw new ValidacaoValorException("CNPJ inválido");
        }

        return cnpjLimpo;
    }

    private boolean isValidCNPJ(String cnpj) {
        if (cnpj == null || cnpj.length() != 14) {
            return false;
        }

        // Verifica se todos os dígitos são iguais
        if (cnpj.matches("(\\d)\\1{13}")) {
            return false;
        }

        try {
            // Calcula primeiro dígito verificador
            int soma = 0;
            int[] peso1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
            for (int i = 0; i < 12; i++) {
                soma += Character.getNumericValue(cnpj.charAt(i)) * peso1[i];
            }
            int primeiroDigito = 11 - (soma % 11);
            if (primeiroDigito >= 10) {
                primeiroDigito = 0;
            }

            // Calcula segundo dígito verificador
            soma = 0;
            int[] peso2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
            for (int i = 0; i < 13; i++) {
                soma += Character.getNumericValue(cnpj.charAt(i)) * peso2[i];
            }
            int segundoDigito = 11 - (soma % 11);
            if (segundoDigito >= 10) {
                segundoDigito = 0;
            }

            // Verifica se os dígitos calculados correspondem aos dígitos do CNPJ
            return Character.getNumericValue(cnpj.charAt(12)) == primeiroDigito &&
                    Character.getNumericValue(cnpj.charAt(13)) == segundoDigito;
        } catch (InputMismatchException e) {
            return false;
        }
    }

    public String getFormatado() {
        if (valor == null || valor.length() != 14) {
            return valor;
        }
        return valor.substring(0, 2) + "." +
                valor.substring(2, 5) + "." +
                valor.substring(5, 8) + "/" +
                valor.substring(8, 12) + "-" +
                valor.substring(12);
    }

    @Override
    public String toString() {
        return getFormatado();
    }
}

