package com.allinfo.model.service;


import com.allinfo.model.domain.Pagination;
import com.allinfo.model.domain.board.BoardDTO;
import com.allinfo.model.domain.board.BoardKindDTO;
import com.allinfo.model.domain.user.UserDTO;
import com.allinfo.model.mapper.BoardMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BoardServiceImpl implements BoardService {
    private final BoardMapper boardMapper;

    @Override
    public List<BoardKindDTO> getBoard() throws Exception {
        return boardMapper.getBoard();
    }

    @Override
    @Transactional
    public Boolean createBoard(BoardDTO board, UserDTO auth) throws Exception {
        board.setUser_uid(auth.getUid());
        boardMapper.createBoard(board);
        return true;
    }

    @Override
    public List<BoardDTO> getBoardList(Long boardKindUid, Pagination pagination) {
        return boardMapper.getBoardList(boardKindUid, pagination);
    }
}
