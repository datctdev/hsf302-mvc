package com.hsf.e_comerce.chatbot.repository;

import com.hsf.e_comerce.chatbot.entity.ChatbotNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatbotNodeRepository extends JpaRepository<ChatbotNode, String> {

    Optional<ChatbotNode> findById(String id);

    /** Root node for role (e.g. NODE_GREETING_GUEST). roleContext = GUEST, BUYER, SELLER, ADMIN. */
    List<ChatbotNode> findByRoleContextOrderBySortOrderAsc(String roleContext);

    /** First root node for role — used as entry point. */
    Optional<ChatbotNode> findFirstByRoleContextOrderBySortOrderAsc(String roleContext);
}
