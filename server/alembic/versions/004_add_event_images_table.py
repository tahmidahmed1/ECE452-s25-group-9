"""add event images table

Revision ID: 004
Revises: 003
Create Date: 2025-01-17 00:00:00.000000

"""
from alembic import op
import sqlalchemy as sa

# revision identifiers, used by Alembic.
revision = '004'
down_revision = '003'
branch_labels = None
depends_on = None

def upgrade():
    # Create event_images table
    op.create_table(
        'event_images',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('event_id', sa.Integer(), nullable=False),
        sa.Column('image_url', sa.String(), nullable=False),
        sa.Column('is_main', sa.Boolean(), nullable=False, default=False),
        sa.Column('display_order', sa.Integer(), nullable=False, default=0),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=True),
        sa.ForeignKeyConstraint(['event_id'], ['events.id'], ondelete='CASCADE'),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_event_images_id'), 'event_images', ['id'], unique=False)
    op.create_index(op.f('ix_event_images_event_id'), 'event_images', ['event_id'], unique=False)
    op.create_index(op.f('ix_event_images_is_main'), 'event_images', ['is_main'], unique=False)

def downgrade():
    # Drop indexes first
    op.drop_index(op.f('ix_event_images_is_main'), table_name='event_images')
    op.drop_index(op.f('ix_event_images_event_id'), table_name='event_images')
    op.drop_index(op.f('ix_event_images_id'), table_name='event_images')
    # Drop table
    op.drop_table('event_images')
