package com.equipe.projetoadsapi.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.equipe.projetoadsapi.model.Usuario;
import com.equipe.projetoadsapi.service.UsuarioService;

/**
 * CONTROLLER DA ENTIDADE USUÁRIO (API RESTful)
 * * Roteiro de Defesa para a Banca: "Implementamos a arquitetura em camadas rígidas 
 * (Controller -> Service -> Repository). Esta classe não possui contato direto com o 
 * Banco de Dados. Sua única responsabilidade é interceptar as requisições HTTP, 
 * delegar a regra de negócio para a camada de Serviço e devolver a resposta encapsulada 
 * no padrão ResponseEntity, garantindo o uso semântico correto dos verbos HTTP."
 */
@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*") // 🟢 Liberação do CORS para o React Native conectar sem bloqueios
public class UsuarioController {

    @Autowired
    private UsuarioService service;

    /**
     * ROTA 1: CADASTRO DE USUÁRIO
     * * Defesa: "Utilizamos a anotação @RequestBody para desserializar o JSON recebido 
     * do mobile. Quando a operação é bem sucedida, retornamos o status 201 (CREATED), 
     * que é o padrão internacional da web para criação de novos recursos."
     */
    @PostMapping("/cadastro")
    public ResponseEntity<Usuario> cadastrar(@RequestBody Usuario novoUsuario) {
        Usuario usuarioCriado = service.cadastrarUsuario(novoUsuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioCriado);
    }

    /**
     * ROTA 2: AUTENTICAÇÃO (LOGIN)
     * * Defesa: "Aqui evitamos a falha de 'Falso Positivo'. Se as credenciais não 
     * baterem no banco, o sistema força o retorno do erro 401 (Unauthorized), 
     * bloqueando o aplicativo de criar uma sessão vazia localmente."
     */
    @PostMapping("/login")
    public ResponseEntity<?> loginUsuario(@RequestBody Usuario loginUsuario) {
        Usuario usuarioAutenticado = service.consultaUsuario(loginUsuario);
        
        if (usuarioAutenticado != null) {
            // Sucesso: Devolve o JSON com os dados do usuário (inclusive o ID)
            return ResponseEntity.ok(usuarioAutenticado);
        } else {
            // Falha: Devolve erro 401 para o React Native exibir o alerta "Senha Incorreta"
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("E-mail ou senha incorretos.");
        }
    }

    /**
     * ROTA 3: BUSCAR USUÁRIO POR ID (Usado pela Tela de Perfil)
     * * Defesa: "Utilizamos o verbo GET (Idempotente) para buscar a 'Pessoa Física'. 
     * A variável de caminho (@PathVariable) extrai o ID da URL enviada pelo aplicativo."
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        Usuario usuarioEncontrado = service.buscarPorId(id);
        
        if (usuarioEncontrado != null) {
            // Envia o Nome e o E-mail de volta para preencher a tela do celular
            return ResponseEntity.ok(usuarioEncontrado);
        } else {
            // Se o ID não existir, retorna erro 404 (Not Found)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuário não encontrado no banco de dados.");
        }
    }
    /**
     * ROTA 4: ATUALIZAR DADOS PESSOAIS
     * Endpoint: PUT /api/usuarios/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> atualizarPerfil(@PathVariable Long id, @RequestBody Usuario dadosAtualizados) {
        try {
            Usuario usuarioSalvo = service.atualizarDados(id, dadosAtualizados);
            return ResponseEntity.ok(usuarioSalvo);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * ROTA 5: ALTERAR SENHA
     * Endpoint: PUT /api/usuarios/{id}/senha
     */
    @PutMapping("/{id}/senha")
    public ResponseEntity<?> trocarSenha(@PathVariable Long id, @RequestBody java.util.Map<String, String> senhas) {
        try {
            service.alterarSenha(id, senhas);
            return ResponseEntity.ok("Senha alterada com sucesso!");
        } catch (RuntimeException e) {
            // Se a senha atual estiver errada, devolvemos Status 400 Bad Request
            return ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * ROTA 6: ATUALIZAR APRESENTAÇÃO DO ALUNO (Com Lista de Interesses)
     * * Roteiro de Defesa: "Utilizamos a anotação @SuppressWarnings('unchecked') 
     * de forma consciente para suprimir o alerta de 'Type Erasure' do compilador Java, 
     * uma vez que o payload do Front-end garante a entrega de um Array de Inteiros 
     * através da estrutura do JSON."
     */
    @SuppressWarnings("unchecked") // 🟢 Adicionado para limpar o aviso amarelo
    @PutMapping("/{id}/aluno")
    public ResponseEntity<?> salvarPerfilAluno(@PathVariable Long id, @RequestBody java.util.Map<String, Object> payload) {
        try {
            // Lendo a string da Bio
            String bioExtraida = (String) payload.get("bio");
            
            // Lendo a Array de números (IDs das matérias) enviada pelo celular
            java.util.List<Integer> interessesIds = (java.util.List<Integer>) payload.get("interessesIds");
            
            Usuario usuarioSalvo = service.atualizarBioAluno(id, bioExtraida, interessesIds);
            return ResponseEntity.ok(usuarioSalvo);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    /**
     * ROTA 7: ATUALIZAR APRESENTAÇÃO DO PROFESSOR (Com Lista de Aptidões)
     */
    @SuppressWarnings("unchecked")
    @PutMapping("/{id}/professor")
    public ResponseEntity<?> salvarPerfilProfessor(@PathVariable Long id, @RequestBody java.util.Map<String, Object> payload) {
        try {
            String bioExtraida = (String) payload.get("bio");
            java.util.List<java.util.Map<String, Object>> aptidoesPayload = (java.util.List<java.util.Map<String, Object>>) payload.get("aptidoesNova");
            
            Usuario usuarioSalvo = service.atualizarPerfilProfessor(id, bioExtraida, aptidoesPayload);
            return ResponseEntity.ok(usuarioSalvo);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}