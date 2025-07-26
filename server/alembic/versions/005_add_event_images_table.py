"""Add event images table

Revision ID: 005_add_event_images_table
Revises: 004_add_subscriptions_table
Create Date: 2025-01-19 10:00:00.000000

"""
from alembic import op
import sqlalchemy as sa

# revision identifiers, used by Alembic.
revision = '005_add_event_images_table'
down_revision = '004_add_subscriptions_table'
branch_labels = None
depends_on = None


def upgrade() -> None:
    # Create event_images table
    op.create_table('event_images',
    sa.Column('id', sa.Integer(), nullable=False),
    sa.Column('event_id', sa.Integer(), nullable=False),
    sa.Column('image_url', sa.String(), nullable=False),
    sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
    sa.ForeignKeyConstraint(['event_id'], ['events.id'], ondelete='CASCADE'),
    sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_event_images_id'), 'event_images', ['id'], unique=False)


def downgrade() -> None:
    # Drop event_images table
    op.drop_index(op.f('ix_event_images_id'), table_name='event_images')
    op.drop_table('event_images')
