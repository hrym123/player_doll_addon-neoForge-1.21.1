package com.lanye.dolladdon.impl.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 自定义方块
 * 禁用对周围方块的遮蔽，使其不遮挡相邻方块
 */
public class CustomBlock extends Block {
    
    public CustomBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }
    
    /**
     * 禁用使用形状进行光照遮蔽
     * 返回false表示不使用方块的形状来计算光照遮蔽
     */
    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return false;
    }
    
    /**
     * 获取方块的遮蔽形状（用于光照计算）
     * 返回空形状，表示不遮蔽光线
     */
    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }
    
    /**
     * 获取方块的视觉形状（用于渲染）
     * 返回完整方块形状，保持正常的渲染
     */
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }
}

