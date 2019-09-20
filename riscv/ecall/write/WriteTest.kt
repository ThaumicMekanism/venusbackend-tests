/* ktlint-disable package-name */
package venusbackend.venusbackend.riscv.ecall.write
/* ktlint-enable package-name */

/* ktlint-disable no-wildcard-imports */
import venus.vfs.*
/* ktlint-enable no-wildcard-imports */
import venusbackend.assembler.Assembler
import venusbackend.linker.Linker
import venusbackend.linker.ProgramAndLibraries
import venusbackend.simulator.Simulator
import kotlin.test.Test
import kotlin.test.assertEquals

class WriteTest {
    @Test
    fun matrixwrite() {
        val (wrepo, wrepoe) = Assembler.assemble(write_repo, "write_repo.s")
        assertEquals(wrepoe.size, 0)
        val (wmatrix, wmatrixe) = Assembler.assemble(write_matrix, "write_matrix.s")
        assertEquals(wmatrixe.size, 0)
        val (ut, ute) = Assembler.assemble(utils, "utils.s")
        assertEquals(ute.size, 0)
        val PandL = ProgramAndLibraries(listOf(wrepo, wmatrix, ut), VirtualFileSystem("dummy"))
        val linked = Linker.link(PandL)
        val sim = Simulator(linked)
        sim.run()
        var f = sim.VFS.getObjectFromPath(output_file_name) ?: VFSDummy()
        assertEquals(f.type, VFSType.File)
        val bytes = (f as VFSFile).readText()
        assertEquals(expected_out, bytes)
    }
}

val output_file_name = "final_output.bin"
val expected_out = "      ���?��@33�@���@33@  �@���@ff�?��Y@"
// val expected_out = File("src/test/kotlin/venusbackend/riscv/ecall/write/final_output.bin").readText(Charsets.UTF_8)
val write_repo = """.import write_matrix.s
.import utils.s

.data
output_path: .asciiz """" + output_file_name + """"
v0: .float 1.2 2.4 5.6 6.4 2.3 4.5 6.4 1.3 3.4

.text
main:

    # =====================================
    # WRITE MATRIX
    # =====================================

    la a0 output_path
    la a1 v0
    addi a2 x0 3
    addi a3 x0 3
    jal write_matrix

    addi a0 x0 10
    ecall
"""
val write_matrix = """.globl write_matrix

.data
error_string: .asciiz "Error occurred writing to or closing the file!"

.text
# ==============================================================================
# FUNCTION: Writes a matrix of floats into a binary file
# FILE FORMAT:
#    The first 8 bytes of the file will be two 4 byte ints representing the
#    numbers of rows and columns respectively. Every 4 bytes thereafter is an
#    element of the matrix in row-major order.
# Arguments:
#     a0 is the pointer to string representing the filename
#    a1 is the pointer to the start of the matrix in memory
#   a2 is the number of rows in the matrix
#   a3 is the number of columns in the matrix
# Returns:
#    a0 is the pointer to the matrix in memory
# ==============================================================================
write_matrix:
    # Prologue
    addi sp sp -8
    # [sp, sp + 8) are a buffer for reading the matrix dimensions

    # Save arguments in temp registers
    mv t0 a0
    mv t1 a1
    mv t2 a2
    mv t3 a3

    #Open file with write permissions
    addi a0 x0 13
    mv a1 t0
    addi a2 x0 1
    ecall

    mv t4 a0
    #t4 is now file descriptor

    # Store matrix dimensions to memory
    sw t2 0(sp)
    sw t3 4(sp)

    # Write matrix dimensions to file
    addi a0 x0 15
    mv a1 t4
    mv a2 sp
    addi a3 x0 8
    addi a4 x0 1
    ecall

    # Calculate size of matrix in bytes
    mul t5 t2 t3
    slli t5 t5 2

    # Write matrix to file
    addi a0 x0 15
    mv a1 t4
    mv a2 t1
    mv a3 t5
    addi a4 x0 1
    ecall

    bne a0 a3 eof_or_error

    # Close file
    addi a0 x0 16
    ecall
    bnez a0 eof_or_error

    #exit
    jr ra


eof_or_error:
    addi a0 x0 4
    la a1 error_string
    ecall
"""
val utils = """.globl print_hex_array malloc

.data
newline: .asciiz "\n"

.text
# ==============================================================================
# FUNCTION: Allocates heap memory and return a pointer to it
# Arguments:
# 	a0 is the # of bytes to allocate heap memory for
# Returns:
#	a0 is the pointer to the allocated heap memory
# ==============================================================================
malloc:
	# Call to sbrk
    mv a1 a0
    addi a0 x0 9
    ecall
    jr ra




# ==============================================================================
# FUNCTION: Prints an array, printing hex values for every 4 bytes of the array
# Arguments:
# 	a0 is the pointer to the start of the array
#	a1 is the number of 4 byte elements in the array
# Returns:
#	None
# ==============================================================================
print_hex_array:
	mv t0 a0
    mv t1 a1

	# Set loop index
    addi t2 x0 0
loop_start:
    beq t1 t2 loop_end
    # Get address of element i
    slli t3 t2 2
    add t3 t0 t3

    # Print element in hex
    addi a0 x0 34
    lw a1 0(t3) # load element from memory
    ecall

    # Print newline
    addi a0 x0 4
    la a1 newline
    ecall

    addi t2 t2 1
    j loop_start
loop_end:
	jr ra
"""
