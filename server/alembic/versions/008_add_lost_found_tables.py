"""Add lost found tables

Revision ID: 008_add_lost_found_tables
Revises: 007_add_notifications_table
Create Date: 2025-01-20 12:00:00.000000

"""
from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

# revision identifiers, used by Alembic.
revision = '008_add_lost_found_tables'
down_revision = '007_add_notifications_table'
branch_labels = None
depends_on = None


def upgrade() -> None:
    # Create lost_found_items table
    op.create_table('lost_found_items',
    sa.Column('id', sa.Integer(), nullable=False),
    sa.Column('user_id', sa.Integer(), nullable=False),
    sa.Column('title', sa.String(), nullable=False),
    sa.Column('description', sa.Text(), nullable=False),
    sa.Column('location', sa.String(), nullable=False),
    sa.Column('item_type', sa.String(), nullable=False),
    sa.Column('reward', sa.String(), nullable=True),
    sa.Column('tags', sa.JSON(), nullable=True),
    sa.Column('expiry_days', sa.Integer(), nullable=False),
    sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
    sa.Column('expires_at', sa.DateTime(timezone=True), nullable=True),
    sa.Column('is_resolved', sa.Boolean(), nullable=False),
    sa.Column('is_active', sa.Boolean(), nullable=False),
    sa.ForeignKeyConstraint(['user_id'], ['users.id'], ondelete='CASCADE'),
    sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_lost_found_items_id'), 'lost_found_items', ['id'], unique=False)
    
    # Create lost_found_images table
    op.create_table('lost_found_images',
    sa.Column('id', sa.Integer(), nullable=False),
    sa.Column('lost_found_item_id', sa.Integer(), nullable=False),
    sa.Column('image_url', sa.String(), nullable=False),
    sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
    sa.ForeignKeyConstraint(['lost_found_item_id'], ['lost_found_items.id'], ondelete='CASCADE'),
    sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_lost_found_images_id'), 'lost_found_images', ['id'], unique=False)


def downgrade() -> None:
    # Drop lost_found_images table
    op.drop_index(op.f('ix_lost_found_images_id'), table_name='lost_found_images')
    op.drop_table('lost_found_images')
    
    # Drop lost_found_items table  
    op.drop_index(op.f('ix_lost_found_items_id'), table_name='lost_found_items')
    op.drop_table('lost_found_items')
