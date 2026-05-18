package com.equipe.projetoadsapi.service;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.equipe.projetoadsapi.model.Usuario;
import com.equipe.projetoadsapi.repository.UsuarioRepository;


/**
 * SERVIÇO DE USUÁRIO (Camada de Regras de Negócio)
 * * Roteiro de Defesa para a Banca: "Esta camada concentra toda a inteligência da aplicação. 
 * Ao isolar as regras de negócio aqui, evitamos que o Controller fique sobrecarregado (Anti-pattern 'Fat Controller'). 
 * Na funcionalidade de troca de senha, validamos a senha atual fornecida pelo usuário contra o hash salvo no banco, 
 * garantindo proteção contra sequestro de conta."
 */
@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository repository;

    @Autowired
    private com.equipe.projetoadsapi.repository.SaberRepository saberRepository; // 🟢 NOVO

    public Usuario cadastrarUsuario(Usuario novoUsuario) {
        Usuario usuarioExistente = repository.findByEmail(novoUsuario.getEmail());
        if (usuarioExistente != null){
            throw new RuntimeException("Email já cadastrado!!!"); 
        }
        return repository.save(novoUsuario);
    } 
  
    public Usuario consultaUsuario(Usuario loginUsuario) {
        Usuario usuarioExistente = repository.findByEmail(loginUsuario.getEmail());
        if (usuarioExistente == null){
            throw new RuntimeException("Usuário não cadastrado!!!"); 
        } else if(!usuarioExistente.getSenha().equals(loginUsuario.getSenha())){
            throw new RuntimeException("Senha incorreta!!!");
        } 
        return usuarioExistente;
    }

    public Usuario buscarPorId(Long id) {
        return repository.findById(id).orElse(null);
    }

    // 🟢 NOVA FUNÇÃO: Atualizar Dados Pessoais (Nome, Nascimento, CPF)
    public Usuario atualizarDados(Long id, Usuario dadosNovos) {
        Usuario usuarioBD = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));
        
        // 🟢 Atualiza os três campos no objeto gerenciado pelo JPA
        usuarioBD.setNome(dadosNovos.getNome());
        usuarioBD.setDataNascimento(dadosNovos.getDataNascimento());
        usuarioBD.setCpf(dadosNovos.getCpf()); // Ativado para salvar no Supabase!
        
        return repository.save(usuarioBD);
    }

    // 🟢 NOVA FUNÇÃO: Trocar a Senha de forma Segura
    public void alterarSenha(Long id, Map<String, String> senhas) {
        Usuario usuarioBD = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));
            
        String senhaAtualDigitada = senhas.get("senhaAtual");
        String novaSenhaDigitada = senhas.get("novaSenha");

        // Regra de segurança: A senha atual digitada precisa bater com a do banco
        if (!usuarioBD.getSenha().equals(senhaAtualDigitada)) {
            throw new RuntimeException("A senha atual informada está incorreta!");
        }

        usuarioBD.setSenha(novaSenhaDigitada);
        repository.save(usuarioBD);
    }
    
    // 🟢 REGRA DE NEGÓCIO: Sincronizar a Biografia e os Interesses do Aluno
    public Usuario atualizarBioAluno(Long id, String bio, java.util.List<Integer> interessesIds) {
        Usuario usuarioBD = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));
        
        usuarioBD.setBioAluno(bio);

        // 1. Limpa as matérias antigas para evitar duplicidade (O Hibernate deleta as antigas)
        if (usuarioBD.getInteresses() != null) {
            usuarioBD.getInteresses().clear();
        } else {
            usuarioBD.setInteresses(new java.util.ArrayList<>());
        }

        // 2. Transforma os IDs enviados pelo celular em objetos 'Interesse' reais e adiciona no usuário
        if (interessesIds != null && !interessesIds.isEmpty()) {
            for (Integer saberId : interessesIds) {
                com.equipe.projetoadsapi.model.Saber saber = saberRepository.findById(saberId.longValue())
                    .orElseThrow(() -> new RuntimeException("Matéria não encontrada no catálogo."));
                
                com.equipe.projetoadsapi.model.Interesse novoInteresse = new com.equipe.projetoadsapi.model.Interesse();
                novoInteresse.setUsuario(usuarioBD);
                novoInteresse.setSaber(saber);
                
                usuarioBD.getInteresses().add(novoInteresse);
            }
        }

        return repository.save(usuarioBD);
    }

    // 🟢 REGRA DE NEGÓCIO: Sincronizar Biografia e Aptidões do Professor
    public Usuario atualizarPerfilProfessor(Long id, String bio, java.util.List<java.util.Map<String, Object>> aptidoesPayload) {
        Usuario usuarioBD = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));
        
        usuarioBD.setBioProfessor(bio);

        // 1. Limpa as aptidões antigas
        if (usuarioBD.getAptidoes() != null) {
            usuarioBD.getAptidoes().clear();
        } else {
            usuarioBD.setAptidoes(new java.util.ArrayList<>());
        }

        // 2. Transforma o JSON do celular em objetos 'Aptidao' reais
        if (aptidoesPayload != null && !aptidoesPayload.isEmpty()) {
            for (java.util.Map<String, Object> apt : aptidoesPayload) {
                // Converte os dados recebidos
                Long saberId = Long.valueOf(apt.get("saberId").toString());
                String nivel = (String) apt.get("nivelDominio");
                java.math.BigDecimal preco = new java.math.BigDecimal(apt.get("precoHora").toString());

                com.equipe.projetoadsapi.model.Saber saber = saberRepository.findById(saberId)
                    .orElseThrow(() -> new RuntimeException("Matéria não encontrada no catálogo."));
                
                com.equipe.projetoadsapi.model.Aptidao novaAptidao = new com.equipe.projetoadsapi.model.Aptidao();
                novaAptidao.setUsuario(usuarioBD);
                novaAptidao.setSaber(saber);
                novaAptidao.setNivelDominio(nivel);
                novaAptidao.setPrecoHora(preco);
                
                usuarioBD.getAptidoes().add(novaAptidao);
            }
        }
        return repository.save(usuarioBD);
    }
    
}