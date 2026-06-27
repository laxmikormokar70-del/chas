package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

val BackgroundGradientStart = Color(0xFF1D2B24)
val BackgroundGradientEnd = Color(0xFF2D4637)
val EmeraldGreen = Color(0xFF6DBE45)
val LightSquareColor = Color(0xFFE8F0E4)
val DarkSquareColor = Color(0xFF688970)
val SelectedHighlight = Color(0x666DBE45)
val ValidMoveHighlight = Color(0x44FFFFFF)
val TextColorPrimary = Color(0xFFFFFFFF)
val TextColorSecondary = Color(0xFFA0C0AE)

enum class PieceType { PAWN, KNIGHT, BISHOP, ROOK, QUEEN, KING }
enum class PieceColor { WHITE, BLACK }

data class ChessPiece(val type: PieceType, val color: PieceColor) {
    val symbol: String
        get() = when (color) {
            PieceColor.WHITE -> when (type) {
                PieceType.KING -> "♔"
                PieceType.QUEEN -> "♕"
                PieceType.ROOK -> "♖"
                PieceType.BISHOP -> "♗"
                PieceType.KNIGHT -> "♘"
                PieceType.PAWN -> "♙"
            }
            PieceColor.BLACK -> when (type) {
                PieceType.KING -> "♚"
                PieceType.QUEEN -> "♛"
                PieceType.ROOK -> "♜"
                PieceType.BISHOP -> "♝"
                PieceType.KNIGHT -> "♞"
                PieceType.PAWN -> "♟"
            }
        }
}

data class Position(val row: Int, val col: Int)

data class ChessState(
    val board: Map<Position, ChessPiece> = initialBoard(),
    val turn: PieceColor = PieceColor.WHITE,
    val selectedPosition: Position? = null,
    val validMoves: List<Position> = emptyList(),
    val whiteCaptured: List<ChessPiece> = emptyList(),
    val blackCaptured: List<ChessPiece> = emptyList(),
    val status: String = "White's Turn"
)

fun initialBoard(): Map<Position, ChessPiece> {
    val board = mutableMapOf<Position, ChessPiece>()
    // Black pieces
    board[Position(0, 0)] = ChessPiece(PieceType.ROOK, PieceColor.BLACK)
    board[Position(0, 1)] = ChessPiece(PieceType.KNIGHT, PieceColor.BLACK)
    board[Position(0, 2)] = ChessPiece(PieceType.BISHOP, PieceColor.BLACK)
    board[Position(0, 3)] = ChessPiece(PieceType.QUEEN, PieceColor.BLACK)
    board[Position(0, 4)] = ChessPiece(PieceType.KING, PieceColor.BLACK)
    board[Position(0, 5)] = ChessPiece(PieceType.BISHOP, PieceColor.BLACK)
    board[Position(0, 6)] = ChessPiece(PieceType.KNIGHT, PieceColor.BLACK)
    board[Position(0, 7)] = ChessPiece(PieceType.ROOK, PieceColor.BLACK)
    for (i in 0..7) board[Position(1, i)] = ChessPiece(PieceType.PAWN, PieceColor.BLACK)

    // White pieces
    for (i in 0..7) board[Position(6, i)] = ChessPiece(PieceType.PAWN, PieceColor.WHITE)
    board[Position(7, 0)] = ChessPiece(PieceType.ROOK, PieceColor.WHITE)
    board[Position(7, 1)] = ChessPiece(PieceType.KNIGHT, PieceColor.WHITE)
    board[Position(7, 2)] = ChessPiece(PieceType.BISHOP, PieceColor.WHITE)
    board[Position(7, 3)] = ChessPiece(PieceType.QUEEN, PieceColor.WHITE)
    board[Position(7, 4)] = ChessPiece(PieceType.KING, PieceColor.WHITE)
    board[Position(7, 5)] = ChessPiece(PieceType.BISHOP, PieceColor.WHITE)
    board[Position(7, 6)] = ChessPiece(PieceType.KNIGHT, PieceColor.WHITE)
    board[Position(7, 7)] = ChessPiece(PieceType.ROOK, PieceColor.WHITE)

    return board
}

class ChessViewModel : ViewModel() {
    private val _state = MutableStateFlow(ChessState())
    val state: StateFlow<ChessState> = _state.asStateFlow()

    fun onSquareClicked(row: Int, col: Int) {
        val pos = Position(row, col)
        val currentState = _state.value
        val clickedPiece = currentState.board[pos]

        if (currentState.selectedPosition == null) {
            // Select piece if it belongs to current player
            if (clickedPiece != null && clickedPiece.color == currentState.turn) {
                val validMoves = calculateValidMoves(pos, clickedPiece, currentState.board)
                _state.update { it.copy(selectedPosition = pos, validMoves = validMoves) }
            }
        } else {
            // Try to move
            if (currentState.validMoves.contains(pos)) {
                // Move is valid
                val newBoard = currentState.board.toMutableMap()
                val pieceToMove = newBoard.remove(currentState.selectedPosition)!!
                val capturedPiece = newBoard.put(pos, pieceToMove)
                
                val newWhiteCaptured = currentState.whiteCaptured.toMutableList()
                val newBlackCaptured = currentState.blackCaptured.toMutableList()
                
                if (capturedPiece != null) {
                    if (capturedPiece.color == PieceColor.WHITE) newWhiteCaptured.add(capturedPiece)
                    else newBlackCaptured.add(capturedPiece)
                    SoundManager.playCaptureSound()
                } else {
                    SoundManager.playMoveSound()
                }

                val nextTurn = if (currentState.turn == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE
                
                _state.update {
                    it.copy(
                        board = newBoard,
                        turn = nextTurn,
                        selectedPosition = null,
                        validMoves = emptyList(),
                        whiteCaptured = newWhiteCaptured,
                        blackCaptured = newBlackCaptured,
                        status = if (nextTurn == PieceColor.WHITE) "White's Turn" else "Black's Turn"
                    )
                }
            } else if (clickedPiece != null && clickedPiece.color == currentState.turn) {
                // Change selection
                val validMoves = calculateValidMoves(pos, clickedPiece, currentState.board)
                _state.update { it.copy(selectedPosition = pos, validMoves = validMoves) }
            } else {
                // Deselect
                _state.update { it.copy(selectedPosition = null, validMoves = emptyList()) }
            }
        }
    }

    fun resetGame() {
        _state.value = ChessState()
    }

    // Simplified move validation (Sandbox-ish but prevents friendly fire)
    private fun calculateValidMoves(pos: Position, piece: ChessPiece, board: Map<Position, ChessPiece>): List<Position> {
        val moves = mutableListOf<Position>()
        // This is a simplified move generator that acts as a sandbox for the purpose of the UI.
        // In a full game, this would contain piece-specific logic (sliding, stepping).
        // Here, we just allow moving anywhere that isn't occupied by a friendly piece, to demonstrate the UI.
        // We will add basic pawn behavior for some realism if needed, but for now sandbox mode is best.
        
        // Actually, let's implement true basic move rules for a better experience.
        when (piece.type) {
            PieceType.PAWN -> {
                val dir = if (piece.color == PieceColor.WHITE) -1 else 1
                // Forward
                val forward = Position(pos.row + dir, pos.col)
                if (forward.row in 0..7 && board[forward] == null) {
                    moves.add(forward)
                    // Initial double move
                    if ((piece.color == PieceColor.WHITE && pos.row == 6) || (piece.color == PieceColor.BLACK && pos.row == 1)) {
                        val doubleForward = Position(pos.row + 2 * dir, pos.col)
                        if (board[doubleForward] == null) {
                            moves.add(doubleForward)
                        }
                    }
                }
                // Capture
                val capLeft = Position(pos.row + dir, pos.col - 1)
                val capRight = Position(pos.row + dir, pos.col + 1)
                if (capLeft.col >= 0 && board[capLeft]?.color != null && board[capLeft]?.color != piece.color) moves.add(capLeft)
                if (capRight.col <= 7 && board[capRight]?.color != null && board[capRight]?.color != piece.color) moves.add(capRight)
            }
            PieceType.KNIGHT -> {
                val jumps = listOf(-2 to -1, -2 to 1, -1 to -2, -1 to 2, 1 to -2, 1 to 2, 2 to -1, 2 to 1)
                for (j in jumps) {
                    val r = pos.row + j.first
                    val c = pos.col + j.second
                    if (r in 0..7 && c in 0..7) {
                        val target = Position(r, c)
                        if (board[target]?.color != piece.color) moves.add(target)
                    }
                }
            }
            PieceType.BISHOP -> addSlidingMoves(pos, piece.color, board, moves, listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1))
            PieceType.ROOK -> addSlidingMoves(pos, piece.color, board, moves, listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1))
            PieceType.QUEEN -> addSlidingMoves(pos, piece.color, board, moves, listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1, -1 to 0, 1 to 0, 0 to -1, 0 to 1))
            PieceType.KING -> {
                val steps = listOf(-1 to -1, -1 to 0, -1 to 1, 0 to -1, 0 to 1, 1 to -1, 1 to 0, 1 to 1)
                for (s in steps) {
                    val r = pos.row + s.first
                    val c = pos.col + s.second
                    if (r in 0..7 && c in 0..7) {
                        val target = Position(r, c)
                        if (board[target]?.color != piece.color) moves.add(target)
                    }
                }
            }
        }
        return moves
    }

    private fun addSlidingMoves(pos: Position, color: PieceColor, board: Map<Position, ChessPiece>, moves: MutableList<Position>, dirs: List<Pair<Int, Int>>) {
        for (dir in dirs) {
            var r = pos.row + dir.first
            var c = pos.col + dir.second
            while (r in 0..7 && c in 0..7) {
                val target = Position(r, c)
                val piece = board[target]
                if (piece == null) {
                    moves.add(target)
                } else {
                    if (piece.color != color) {
                        moves.add(target)
                    }
                    break
                }
                r += dir.first
                c += dir.second
            }
        }
    }
}

@Composable
fun ChessScreen(viewModel: ChessViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(BackgroundGradientStart, BackgroundGradientEnd)
                )
            )
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TopBar()
            
            Spacer(modifier = Modifier.height(24.dp))
            
            PlayerInfo(
                name = "Opponent",
                rating = "1840",
                captured = state.whiteCaptured,
                isCurrentTurn = state.turn == PieceColor.BLACK
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            ChessBoardUI(
                board = state.board,
                selectedPosition = state.selectedPosition,
                validMoves = state.validMoves,
                onSquareClick = { r, c -> viewModel.onSquareClicked(r, c) }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            PlayerInfo(
                name = "You",
                rating = "1920",
                captured = state.blackCaptured,
                isCurrentTurn = state.turn == PieceColor.WHITE
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            GameControls(
                status = state.status,
                onReset = { viewModel.resetGame() }
            )
        }
    }
}

@Composable
fun TopBar() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "ChessMaster",
                color = TextColorPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Local Play Sandbox",
                color = EmeraldGreen,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
        
        IconButton(
            onClick = { /* Settings */ },
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.1f), CircleShape)
        ) {
            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextColorPrimary)
        }
    }
}

@Composable
fun PlayerInfo(name: String, rating: String, captured: List<ChessPiece>, isCurrentTurn: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .border(
                width = if (isCurrentTurn) 2.dp else 0.dp,
                color = if (isCurrentTurn) EmeraldGreen else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.White.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(name.take(1), fontSize = 20.sp, color = TextColorPrimary, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(name, color = TextColorPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text(rating, color = TextColorSecondary, fontSize = 14.sp)
            }
        }
        
        Row {
            // Group captured pieces
            val grouped = captured.groupBy { it.type }
            grouped.forEach { (type, list) ->
                val samplePiece = list.first()
                Text(
                    text = "${samplePiece.symbol}${if (list.size > 1) "x${list.size}" else ""}",
                    color = if (samplePiece.color == PieceColor.WHITE) Color.White else Color.Black,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
            }
        }
    }
}

@Composable
fun ChessBoardUI(
    board: Map<Position, ChessPiece>,
    selectedPosition: Position?,
    validMoves: List<Position>,
    onSquareClick: (Int, Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .border(4.dp, Color(0xFF15201A), RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            for (row in 0..7) {
                Row(modifier = Modifier.weight(1f)) {
                    for (col in 0..7) {
                        val isLightSquare = (row + col) % 2 == 0
                        val pos = Position(row, col)
                        val isSelected = selectedPosition == pos
                        val isValidMove = validMoves.contains(pos)
                        val piece = board[pos]
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(if (isLightSquare) LightSquareColor else DarkSquareColor)
                                .clickable { onSquareClick(row, col) }
                                .testTag("square_${row}_${col}"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(SelectedHighlight)
                                )
                            }
                            if (isValidMove) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(ValidMoveHighlight, CircleShape)
                                )
                            }
                            if (piece != null) {
                                Text(
                                    text = piece.symbol,
                                    fontSize = 32.sp,
                                    color = if (piece.color == PieceColor.WHITE) Color.White else Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GameControls(status: String, onReset: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Game Status", color = TextColorSecondary, fontSize = 14.sp)
            Text(status, color = TextColorPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        
        Button(
            onClick = onReset,
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = "Reset Game", tint = BackgroundGradientStart)
            Spacer(modifier = Modifier.width(8.dp))
            Text("New Game", color = BackgroundGradientStart, fontWeight = FontWeight.Bold)
        }
    }
}
